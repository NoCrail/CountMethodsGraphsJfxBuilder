package controller

import kotlin.math.ln

fun f(x: Double, y:Double): Double{
        return y/(2*x+y-4* ln(x))
    }

    fun eulerRightDiffs(x: DoubleArray, y: DoubleArray, h: Double): DoubleArray{
        for (i in 0..y.size-2){
            y[i+1] = y[i] + h*f(x[i], y[i])
//            print(x[i].toString()+ ", " +y[i].toString())
//            println()
        }
        return y
    }


    fun eulerCentralDiffs(x: DoubleArray, y: DoubleArray, h: Double): DoubleArray{
        y[1] = y[0] + h*f(x[0], y[0])
        for(i in 1..y.size-2){
            y[i+1] = y[i-1] + 2*h*f(x[i], y[i])
        }
        return y
    }

    fun eulerMod(x: DoubleArray, y: DoubleArray, h: Double): DoubleArray{
        for(i in 0..y.size-2){
            val yt = y[i] + h/2 * f(x[i], y[i])
            y[i+1] = y[i] + h*f(x[i]+h/2, yt)
        }
        return y
    }

    fun eulerRecount(x: DoubleArray, y: DoubleArray, h: Double): DoubleArray{
        for(i in 0..y.size-2){
            val yt = y[i] + h * f(x[i], y[i])
            y[i+1] = y[i] + h/2*(f(x[i], y[i]+f(x[i]+ h, yt)))
        }
        return y
    }

    fun rungeKutta(x: DoubleArray, y: DoubleArray, h: Double): DoubleArray {
        for(i in 0..y.size-2) {
            val k0 = f(x[i], y[i])
            val k1 = f(x[i] + h / 2, y[i] + h * k0 / 2)
            val k2 = f(x[i] + h / 2, y[i] + h * k1 / 2)
            val k3 = f(x[i] + h, y[i] + h * k2)
            y[i + 1] = y[i] + h / 6 * (k0 + 2 * k1 + 2 * k2 + k3)
        }
        return y
    }

    fun adams(x: DoubleArray, y: DoubleArray, h: Double): DoubleArray {
        val f = DoubleArray(x.size)
        f[0] = f(x[0],y[0])
        x[1] = x[0]+h
        y[1] = y[0]+h*f[0]
        f[1] = f(x[1],y[1])
        x[2] = x[1]+h
        y[2] = y[1]+h*(3/2*f[1]-1/2*f[0])
        f[2] = f(x[2],y[2])
        x[3] = x[2]+h
        y[3] = y[2]+h/12*(23*f[2]-16*f[1]+5*f[0])
        f[3] = f(x[3],y[3])
        for(i in 3..x.size-2){
            y[i+1] = y[i]+h/24*(55*f[i]-59*f[i-1]+37*f[i-2]-9*f[i-3])
            x[i+1] = x[i]+h
            f[i+1] = f(x[i+1],y[i+1])
        }

        return y
    }


