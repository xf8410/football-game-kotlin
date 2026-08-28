package com.football.game.core

import kotlin.math.sqrt

/**
 * 3D向量数据类
 * 用于表示位置、速度、方向等
 */
data class Vector3(
    val x: Float = 0.0f,
    val y: Float = 0.0f,
    val z: Float = 0.0f
) {
    companion object {
        val ZERO = Vector3(0.0f, 0.0f, 0.0f)
        val FORWARD = Vector3(0.0f, 0.0f, 1.0f)
        val BACKWARD = Vector3(0.0f, 0.0f, -1.0f)
        val UP = Vector3(0.0f, 1.0f, 0.0f)
        val DOWN = Vector3(0.0f, -1.0f, 0.0f)
        val LEFT = Vector3(-1.0f, 0.0f, 0.0f)
        val RIGHT = Vector3(1.0f, 0.0f, 0.0f)
    }

    /**
     * 向量长度
     */
    fun length(): Float {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * 向量长度的平方（避免开方，用于距离比较）
     */
    fun lengthSquared(): Float {
        return x * x + y * y + z * z
    }

    /**
     * 归一化向量
     */
    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.0001f) {
            Vector3(x / len, y / len, z / len)
        } else {
            ZERO
        }
    }

    /**
     * 到另一个向量的距离
     */
    fun distanceTo(other: Vector3): Float {
        return (this - other).length()
    }

    /**
     * 到另一个向量距离的平方
     */
    fun distanceToSquared(other: Vector3): Float {
        return (this - other).lengthSquared()
    }

    /**
     * 线性插值
     */
    fun lerp(target: Vector3, t: Float): Vector3 {
        return Vector3(
            x + (target.x - x) * t,
            y + (target.y - y) * t,
            z + (target.z - z) * t
        )
    }

    /**
     * 限制在2D平面（忽略Y轴）
     */
    fun flatten(): Vector3 {
        return Vector3(x, 0.0f, z)
    }

    // 运算符重载
    operator fun plus(other: Vector3): Vector3 {
        return Vector3(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Vector3): Vector3 {
        return Vector3(x - other.x, y - other.y, z - other.z)
    }

    operator fun times(scalar: Float): Vector3 {
        return Vector3(x * scalar, y * scalar, z * scalar)
    }

    operator fun div(scalar: Float): Vector3 {
        return if (scalar != 0.0f) {
            Vector3(x / scalar, y / scalar, z / scalar)
        } else {
            ZERO
        }
    }

    operator fun unaryMinus(): Vector3 {
        return Vector3(-x, -y, -z)
    }

    /**
     * 点积
     */
    fun dot(other: Vector3): Float {
        return x * other.x + y * other.y + z * other.z
    }

    /**
     * 叉积（2D简化版，只考虑XZ平面）
     */
    fun cross2D(other: Vector3): Float {
        return x * other.z - z * other.x
    }

    /**
     * 角度（弧度）到另一个向量（2D，XZ平面）
     */
    fun angleTo(other: Vector3): Float {
        val dot = this.flatten().dot(other.flatten())
        val lenProduct = this.flatten().length() * other.flatten().length()
        return if (lenProduct > 0.0001f) {
            kotlin.math.acos((dot / lenProduct).coerceIn(-1.0f, 1.0f))
        } else {
            0.0f
        }
    }

    /**
     * 旋转角度（绕Y轴，2D）
     */
    fun rotatedY(angleRadians: Float): Vector3 {
        val cos = kotlin.math.cos(angleRadians.toDouble()).toFloat()
        val sin = kotlin.math.sin(angleRadians.toDouble()).toFloat()
        return Vector3(
            x * cos + z * sin,
            y,
            -x * sin + z * cos
        )
    }
}