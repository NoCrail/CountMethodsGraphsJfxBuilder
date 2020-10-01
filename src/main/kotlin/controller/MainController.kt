package controller

import Plot
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.control.TextField
import java.awt.Color
import java.io.File


class MainController {



    @FXML
    lateinit var buildBtn: Button
    lateinit var imgView: ImageView
    lateinit var xAxisRightField: TextField
    lateinit var yAxisTopField: TextField
    lateinit var xAxisLeftField: TextField
    lateinit var yAxisBotField: TextField
    lateinit var modCheck: CheckBox
    lateinit var recountCheck: CheckBox
    lateinit var rightDiffsCheck: CheckBox
    lateinit var centralDiffsCheck: CheckBox
    lateinit var adamsCheck: CheckBox
    lateinit var rungeKuttaCheck: CheckBox
    lateinit var hField: TextField


    fun initialize() {
        lateinit var plot: Plot
        if(::yAxisBotField.isInitialized) {
            val axisParams = getAxisParams()
            plot = setPlotParams(axisParams)
        }

//        val data = Plot.data().xy(10.0, 2.0).xy(3.0, 4.0)
//        data.xy(3.0, 4.0).xy(4.0, 4.0)
//        plot.series("Data", data,
//                Plot.seriesOpts().marker(Plot.Marker.NONE))


        buildBtn.setOnAction {
            val buildOpts = getBuildOpts()
            val axisParams = getAxisParams()
            plot = setPlotParams(axisParams)

            val h = buildOpts.h
            val x: DoubleArray = DoubleArray((1/h).toInt()+1)
            val y: DoubleArray = DoubleArray((1/h).toInt()+1)
            x[0] = 1.0
            y[0] = 1.0
            for (i in 0..x.size-2){
                x[i+1] = x[i] + h
            }


            if(buildOpts.rightDiffs){
                val data = getRightDiffsData(x, y.clone(), x.size, h)
                plot.series("RightD", data, Plot.seriesOpts().marker(Plot.Marker.NONE).color(Color.BLACK))
            }

            if(buildOpts.centralDiffs){
                val data = getCentralDiffsData(x, y.clone(), x.size, h)
                plot.series("CentralD", data, Plot.seriesOpts().marker(Plot.Marker.NONE).color(Color.CYAN))
            }

            if(buildOpts.mod){
                val data = getModData(x, y.clone(), x.size, h)
                plot.series("Mod", data, Plot.seriesOpts().marker(Plot.Marker.NONE).color(Color.GREEN))
            }

            if(buildOpts.recount){
                val data = getRecountData(x, y.clone(), x.size, h)
                plot.series("Recount", data, Plot.seriesOpts().marker(Plot.Marker.NONE).color(Color.ORANGE))
            }

            if(buildOpts.rungeKutta){
                val data = getRungeKuttaData(x, y.clone(), x.size, h)
                plot.series("RungeKutta", data, Plot.seriesOpts().marker(Plot.Marker.NONE).color(Color.PINK))
            }

            if(buildOpts.adams){
                val data = getAdamsData(x, y.clone(), x.size, h)
                plot.series("Adams", data, Plot.seriesOpts().marker(Plot.Marker.NONE).color(Color.MAGENTA))
            }


            if(buildOpts.notAllEmpty()) {
                plot.save("samp", "png")
                val file = File("samp.png")
                imgView.image = Image(file.toURI().toString())

            } else {
                val alert = Alert(Alert.AlertType.INFORMATION)
                alert.title = "Хоспаде"
                alert.headerText = "Шо за придурок"
                alert.contentText = "Выбери хоть чонить. Чуть прогу не повалил"
                alert.showAndWait()
            }
        }

    }



    data class AxisParams(val xLSize: Double, val xRSize: Double, val yTSize:Double, val yBSize:Double)
    data class BuildOpts(val mod: Boolean, val recount: Boolean, val rightDiffs: Boolean, val centralDiffs: Boolean, val adams: Boolean, val rungeKutta: Boolean, val h: Double){
        fun notAllEmpty(): Boolean{
            return (mod||recount||rightDiffs||centralDiffs||adams||rungeKutta)
        }
    }

    fun getAxisParams(): AxisParams{
        val xLSize = xAxisLeftField.text.toDouble()
        val xRSize = xAxisRightField.text.toDouble()
        val yTSize = yAxisTopField.text.toDouble()
        val yBSize = yAxisBotField.text.toDouble()
        return AxisParams(xLSize, xRSize, yTSize, yBSize)
    }

    fun setPlotParams(axisParams: AxisParams): Plot{
        val plot = Plot.plot(Plot.plotOpts()
                .title("PG")
                .legend(Plot.LegendFormat.BOTTOM))
                .xAxis("x", Plot.axisOpts().range(axisParams.xLSize, axisParams.xRSize))
                .yAxis("y", Plot.axisOpts().range(axisParams.yBSize, axisParams.yTSize))
        return plot
    }

    fun getBuildOpts(): BuildOpts{
        return BuildOpts(modCheck.isSelected, recountCheck.isSelected, rightDiffsCheck.isSelected, centralDiffsCheck.isSelected, adamsCheck.isSelected, rungeKuttaCheck.isSelected, hField.text.toDouble())
    }

    fun getRightDiffsData(x: DoubleArray, y:DoubleArray, n: Int, h: Double): Plot.Data{
        val yRD = eulerRightDiffs(x, y, x.size, h)
        val data = Plot.data()
        for (i in 0..x.size - 1) {
            data.xy(x[i], yRD[i])
        }
        return data
    }

    fun getCentralDiffsData(x: DoubleArray, y:DoubleArray, n: Int, h: Double): Plot.Data{
        val yRD = eulerCentralDiffs(x, y, x.size, h)
        val data = Plot.data()
        for (i in 0..x.size - 1) {
            data.xy(x[i], yRD[i])
        }
        return data
    }

    fun getModData(x: DoubleArray, y:DoubleArray, n: Int, h: Double): Plot.Data{
        val yRD = eulerMod(x, y, x.size, h)
        val data = Plot.data()
        for (i in 0..x.size - 1) {
            data.xy(x[i], yRD[i])
        }
        return data
    }

    fun getRecountData(x: DoubleArray, y:DoubleArray, n: Int, h: Double): Plot.Data{
        val yRD = eulerRecount(x, y, x.size, h)
        val data = Plot.data()
        for (i in 0..x.size - 1) {
            data.xy(x[i], yRD[i])
        }
        return data
    }

    fun getRungeKuttaData(x: DoubleArray, y: DoubleArray, n: Int, h: Double): Plot.Data{
        val yRD = rungeKutta(x, y, x.size, h)
        val data = Plot.data()
        for (i in 0..x.size - 1) {
            data.xy(x[i], yRD[i])
        }
        return data
    }

    fun getAdamsData(x: DoubleArray, y: DoubleArray, n: Int, h: Double): Plot.Data{
        val yRD = adams(x, y, x.size, h)
        val data = Plot.data()
        for (i in 0..x.size - 1) {
            data.xy(x[i], yRD[i])
        }
        return data
    }


}