package controller

    fun f(x: Double, y:Double): Double{
        return y/(2*x+y-4*Math.log(x))
    }

    fun eulerRightDiffs(x: DoubleArray, y:DoubleArray, n: Int, h: Double): DoubleArray{
        for (i in 0..y.size-2){
            y[i+1] = y[i] + h*f(x[i], y[i])
//            print(x[i].toString()+ ", " +y[i].toString())
//            println()
        }
        return y
    }

    fun eulerLeftDiffs(x: DoubleArray, y:DoubleArray, n: Int, h: Double): DoubleArray{
        for(i in 0..y.size-2){
            y[i+1] = y[i] + h*f(x[i+1], y[i+1])
            print(x[i].toString()+ ", " +y[i].toString())
            println()
        }
        return y
    }

    fun eulerCentralDiffs(x: DoubleArray, y:DoubleArray, n: Int, h: Double): DoubleArray{
        y[1] = y[0] + h*f(x[0], y[0])
        for(i in 1..y.size-2){
            y[i+1] = y[i-1] + 2*h*f(x[i], y[i])
        }
        return y
    }

    fun eulerMod(x: DoubleArray, y:DoubleArray, n: Int, h: Double): DoubleArray{
        for(i in 0..y.size-2){
            val yt = y[i] + h/2 * f(x[i], y[i])
            y[i+1] = y[i] + h*f(x[i]+h/2, yt)
        }
        return y
    }

    fun eulerRecount(x: DoubleArray, y:DoubleArray, n: Int, h: Double): DoubleArray{
        for(i in 0..y.size-2){
            val yt = y[i] + h * f(x[i], y[i])
            y[i+1] = y[i] + h/2*(f(x[i], y[i]+f(x[i]+ h, yt)))
        }
        return y
    }
