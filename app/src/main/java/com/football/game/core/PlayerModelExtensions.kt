package com.football.game.core

import kotlin.math.sqrt
import kotlin.math.atan2

/**
 * Vector3 扩展函数
 */
fun Vector3.length(): Float = sqrt(x * x + y * y + z * z)

fun Vector3.lengthSquared(): Float = x * x + y * y + z * z

fun Vector3.normalized(): Vector3 {
    val len = length()
    return if (len > 0.001f) Vector3(x / len, y / len, z / len) else Vector3.ZERO
}

fun Vector3.distanceTo(other: Vector3): Float = (this - other).length()

fun Vector3.dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

fun Vector3.directionTo(other: Vector3): Vector3 = (other - this).normalized()

fun Vector3.lerp(target: Vector3, t: Float): Vector3 {
    return Vector3(
        x + (target.x - x) * t,
        y + (target.y - y) * t,
        z + (target.z - z) * t
    )
}

operator fun Vector3.plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
operator fun Vector3.minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
operator fun Vector3.times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
operator fun Vector3.div(scalar: Float): Vector3 = if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else Vector3.ZERO
operator fun Vector3.unaryMinus(): Vector3 = Vector3(-x, -y, -z)

fun Vector3.cos(angle: Float): Float = kotlin.math.cos(angle.toDouble()).toFloat()
fun Vector3.sin(angle: Float): Float = kotlin.math.sin(angle.toDouble()).toFloat()