package dev.kymera.keyboard.core

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Swipe-to-type engine that interprets swipe paths as words.
 * Uses key proximity and path analysis to determine intended keys.
 */
class SwipeEngine {

    // Minimum distance to consider a swipe (in pixels)
    private val minSwipeDistance = 50f

    // Maximum distance from key center to register a hit
    private val keyHitRadius = 60f

    // Debounce threshold - ignore keys visited too quickly
    private val minTimeBetweenKeys = 30L // ms

    // Current swipe state
    private var swipePath = mutableListOf<SwipePoint>()
    private var visitedKeys = mutableListOf<KeyHit>()
    private var lastKeyTime = 0L

    data class SwipePoint(
        val x: Float,
        val y: Float,
        val timestamp: Long
    )

    data class KeyHit(
        val key: String,
        val confidence: Float,
        val timestamp: Long
    )

    /**
     * Start a new swipe gesture.
     */
    fun startSwipe(x: Float, y: Float) {
        swipePath.clear()
        visitedKeys.clear()
        lastKeyTime = 0L
        addPoint(x, y)
    }

    /**
     * Add a point to the current swipe path.
     */
    fun addPoint(x: Float, y: Float) {
        val timestamp = System.currentTimeMillis()
        swipePath.add(SwipePoint(x, y, timestamp))
    }

    /**
     * End the swipe and get the interpreted word.
     *
     * @param keyRects Map of key labels to their bounding rectangles
     * @return The interpreted word, or null if swipe was too short
     */
    fun endSwipe(keyRects: Map<String, RectF>): String? {
        if (!isValidSwipe()) {
            return null
        }

        // Process path to find visited keys
        processPath(keyRects)

        // Build word from visited keys
        val word = buildWord()

        // Clear state
        swipePath.clear()
        visitedKeys.clear()

        return word
    }

    /**
     * Check if the current path represents a valid swipe.
     */
    fun isValidSwipe(): Boolean {
        if (swipePath.size < 3) return false

        val totalDistance = calculatePathLength()
        return totalDistance >= minSwipeDistance
    }

    private fun calculatePathLength(): Float {
        var length = 0f
        for (i in 1 until swipePath.size) {
            length += distance(swipePath[i - 1], swipePath[i])
        }
        return length
    }

    private fun distance(p1: SwipePoint, p2: SwipePoint): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun processPath(keyRects: Map<String, RectF>) {
        for (point in swipePath) {
            val nearestKey = findNearestKey(point, keyRects)
            if (nearestKey != null) {
                val (key, distance) = nearestKey

                // Calculate confidence based on distance
                val confidence = 1f - (distance / keyHitRadius).coerceIn(0f, 1f)

                // Check if this is a new key or same as last
                val lastKey = visitedKeys.lastOrNull()
                if (lastKey?.key != key) {
                    // Debounce check
                    if (point.timestamp - lastKeyTime >= minTimeBetweenKeys) {
                        visitedKeys.add(KeyHit(key, confidence, point.timestamp))
                        lastKeyTime = point.timestamp
                    }
                } else {
                    // Update confidence if higher
                    if (confidence > lastKey.confidence) {
                        visitedKeys[visitedKeys.lastIndex] = lastKey.copy(confidence = confidence)
                    }
                }
            }
        }
    }

    private fun findNearestKey(
        point: SwipePoint,
        keyRects: Map<String, RectF>
    ): Pair<String, Float>? {
        var nearestKey: String? = null
        var nearestDistance = Float.MAX_VALUE

        for ((key, rect) in keyRects) {
            // Only consider alphabetic keys for swipe
            if (key.length != 1 || !key[0].isLetter()) continue

            val centerX = rect.centerX()
            val centerY = rect.centerY()
            val dx = point.x - centerX
            val dy = point.y - centerY
            val distance = sqrt(dx * dx + dy * dy)

            if (distance < nearestDistance && distance <= keyHitRadius) {
                nearestKey = key
                nearestDistance = distance
            }
        }

        return nearestKey?.let { it to nearestDistance }
    }

    private fun buildWord(): String? {
        if (visitedKeys.isEmpty()) return null

        // Filter out low-confidence hits
        val filteredKeys = visitedKeys.filter { it.confidence > 0.3f }
        if (filteredKeys.isEmpty()) return null

        // Build the word
        return filteredKeys.joinToString("") { it.key.lowercase() }
    }

    /**
     * Get suggestions for the current swipe path.
     * This could be expanded to use a dictionary for better suggestions.
     */
    fun getSuggestions(keyRects: Map<String, RectF>, dictionary: Set<String>? = null): List<String> {
        if (!isValidSwipe()) return emptyList()

        // Process without clearing state
        val originalVisited = visitedKeys.toList()
        processPath(keyRects)
        val word = buildWord()
        visitedKeys.clear()
        visitedKeys.addAll(originalVisited)

        if (word == null) return emptyList()

        // If we have a dictionary, find close matches
        if (dictionary != null) {
            return dictionary
                .filter { it.startsWith(word.take(2)) }
                .sortedBy { levenshteinDistance(word, it) }
                .take(5)
        }

        return listOf(word)
    }

    /**
     * Calculate edit distance between two strings.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    /**
     * Analyze swipe gesture for debugging.
     */
    fun analyzeGesture(): GestureAnalysis {
        return GestureAnalysis(
            pointCount = swipePath.size,
            totalLength = calculatePathLength(),
            keysVisited = visitedKeys.size,
            duration = if (swipePath.size >= 2) {
                swipePath.last().timestamp - swipePath.first().timestamp
            } else 0L
        )
    }

    data class GestureAnalysis(
        val pointCount: Int,
        val totalLength: Float,
        val keysVisited: Int,
        val duration: Long
    )
}
