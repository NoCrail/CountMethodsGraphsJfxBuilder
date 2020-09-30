import java.awt.*
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

/**
 * Simple implementation of plot. Minimal features, no dependencies besides standard libraries.
 * Options are self-descriptive, see also samples.
 *
 * @author Yuriy Guskov
 */
class Plot private constructor(opts: PlotOptions?) {
    enum class Line {
        NONE, SOLID, DASHED
    }

    enum class Marker {
        NONE, CIRCLE, SQUARE, DIAMOND, COLUMN, BAR
    }

    enum class AxisFormat {
        NUMBER, NUMBER_KGM, NUMBER_INT, TIME_HM, TIME_HMS, DATE, DATETIME_HM, DATETIME_HMS
    }

    enum class LegendFormat {
        NONE, TOP, RIGHT, BOTTOM
    }

    private enum class HorizAlign {
        LEFT, CENTER, RIGHT
    }

    private enum class VertAlign {
        TOP, CENTER, BOTTOM
    }

    private var opts = PlotOptions()
    private val boundRect: Rectangle
    private val plotArea: PlotArea
    private val xAxes: MutableMap<String?, Axis> = HashMap(3)
    private val yAxes: MutableMap<String?, Axis> = HashMap(3)
    private val dataSeriesMap: MutableMap<String, DataSeries> = LinkedHashMap(5)

    class PlotOptions {
        var title = ""
        var width = 800
        var height = 600
        var backgroundColor = Color.WHITE
        var foregroundColor = Color.BLACK
        var titleFont = Font("Arial", Font.BOLD, 16)
        var padding = 10 // padding for the entire image
        var plotPadding = 5 // padding for plot area (to have min and max values padded)
        var labelPadding = 10
        val defaultLegendSignSize = 10
        var legendSignSize = 10
        var grids = Point(10, 10) // grid lines by x and y
        var gridColor = Color.GRAY
        var gridStroke: Stroke = BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, floatArrayOf(5.0f), 0.0f)
        var tickSize = 5
        var labelFont = Font("Arial", 0, 12)
        var legend = LegendFormat.NONE
        fun title(title: String): PlotOptions {
            this.title = title
            return this
        }

        fun width(width: Int): PlotOptions {
            this.width = width
            return this
        }

        fun height(height: Int): PlotOptions {
            this.height = height
            return this
        }

        fun bgColor(color: Color): PlotOptions {
            backgroundColor = color
            return this
        }

        fun fgColor(color: Color): PlotOptions {
            foregroundColor = color
            return this
        }

        fun titleFont(font: Font): PlotOptions {
            titleFont = font
            return this
        }

        fun padding(padding: Int): PlotOptions {
            this.padding = padding
            return this
        }

        fun plotPadding(padding: Int): PlotOptions {
            plotPadding = padding
            return this
        }

        fun labelPadding(padding: Int): PlotOptions {
            labelPadding = padding
            return this
        }

        fun labelFont(font: Font): PlotOptions {
            labelFont = font
            return this
        }

        fun grids(byX: Int, byY: Int): PlotOptions {
            grids = Point(byX, byY)
            return this
        }

        fun gridColor(color: Color): PlotOptions {
            gridColor = color
            return this
        }

        fun gridStroke(stroke: Stroke): PlotOptions {
            gridStroke = stroke
            return this
        }

        fun tickSize(value: Int): PlotOptions {
            tickSize = value
            return this
        }

        fun legend(legend: LegendFormat): PlotOptions {
            this.legend = legend
            return this
        }
    }

    fun opts(): PlotOptions {
        return opts
    }

    fun xAxis(name: String, opts: AxisOptions?): Plot {
        xAxes[name] = Axis(name, opts)
        return this
    }

    fun yAxis(name: String, opts: AxisOptions?): Plot {
        yAxes[name] = Axis(name, opts)
        return this
    }

    fun series(name: String, data: Data?, opts: DataSeriesOptions?): Plot {
        var series = dataSeriesMap[name]
        opts?.setPlot(this)
        if (series == null) {
            series = DataSeries(name, data, opts)
            dataSeriesMap[name] = series
        } else {
            series.data = data
            series.opts = opts
        }
        return this
    }

    fun series(name: String, opts: DataSeriesOptions?): Plot {
        val series = dataSeriesMap[name]
        opts?.setPlot(this)
        if (series != null) series.opts = opts
        return this
    }

    private fun calc(g: Graphics2D) {
        plotArea.calc(g)
    }

    private fun clear() {
        plotArea.clear()
        for (series in dataSeriesMap.values) series.clear()
    }

    private fun draw(): BufferedImage {
        val image = BufferedImage(opts.width, opts.height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        return try {
            calc(g)
            drawBackground(g)
            plotArea.draw(g)
            for (series in dataSeriesMap.values) series.draw(g)
            image
        } finally {
            g.dispose()
        }
    }

    private fun drawBackground(g: Graphics2D) {
        g.color = opts.backgroundColor
        g.fillRect(0, 0, opts.width, opts.height)
    }

    @Throws(IOException::class)
    fun save(fileName: String, type: String) {
        clear()
        val bi = draw()
        val outputFile = File("$fileName.$type")
        ImageIO.write(bi, type, outputFile)
    }

    private inner class Legend {
        var rect: Rectangle? = null
        var labelRect: Rectangle2D? = null
        var entryWidth = 0
        var entryWidthPadded = 0
        var entryCount = 0
        var xCount = 0
        var yCount = 0
    }

    private inner class PlotArea {
        private val plotBorderRect = Rectangle() // boundRect | labels/legend | plotBorderRect | plotPadding | plotRect/clipRect
        val plotRect = Rectangle()
        val plotClipRect = Rectangle()
        private val legend: Legend = Legend()
        val xPlotRange = Range(0.0, 0.0)
        val yPlotRange = Range(0.0, 0.0)
        fun clear() {
            plotBorderRect.bounds = boundRect
            plotRectChanged()
        }

        private fun offset(dx: Int, dy: Int, dw: Int, dh: Int) {
            plotBorderRect.translate(dx, dy)
            plotBorderRect.setSize(plotBorderRect.width - dx - dw, plotBorderRect.height - dy - dh)
            plotRectChanged()
        }

        private fun plotRectChanged() {
            plotRect.setBounds(plotBorderRect.x + opts.plotPadding, plotBorderRect.y + opts.plotPadding,
                    plotBorderRect.width - opts.plotPadding * 2, plotBorderRect.height - opts.plotPadding * 2)
            xPlotRange.setMin(plotRect.getX())
            xPlotRange.setMax(plotRect.getX() + plotRect.getWidth())
            yPlotRange.setMin(plotRect.getY())
            yPlotRange.setMax(plotRect.getY() + plotRect.getHeight())
            plotClipRect.setBounds(plotBorderRect.x + 1, plotBorderRect.y + 1, plotBorderRect.width - 1, plotBorderRect.height - 1)
        }

        fun calc(g: Graphics2D) {
            calcAxes(g)
            calcRange(true)
            calcRange(false)
            calcAxisLabels(g, true)
            calcAxisLabels(g, false)
            g.font = opts.titleFont
            var fm = g.fontMetrics
            val titleRect = fm.getStringBounds(opts.title, g)
            g.font = opts.labelFont
            fm = g.fontMetrics
            var xAxesHeight = 0
            var xAxesHalfWidth = 0
            for ((_, xAxis) in xAxes) {
                xAxesHeight += toInt(xAxis.labelRect!!.height) + opts.labelPadding * 2
                if (xAxis.labelRect!!.width > xAxesHalfWidth) xAxesHalfWidth = toInt(xAxis.labelRect!!.width)
            }
            var yAxesWidth = 0
            for ((_, value) in yAxes) yAxesWidth += toInt(value.labelRect!!.width) + opts.labelPadding * 2
            val dx = opts.padding + yAxesWidth
            var dy = opts.padding + toInt(titleRect.height + opts.labelPadding)
            var dw = opts.padding
            if (opts.legend != LegendFormat.RIGHT) dw += xAxesHalfWidth // half of label goes beyond a plot in right bottom corner
            var dh = opts.padding + xAxesHeight
            // offset for legend
            val temp = Rectangle(plotBorderRect) // save plotRect
            offset(dx, dy, dw, dh)
            calcLegend(g) // use plotRect
            plotBorderRect.bounds = temp // restore plotRect
            when (opts.legend) {
                LegendFormat.TOP -> dy += legend.rect!!.height + opts.labelPadding
                LegendFormat.RIGHT -> dw += legend.rect!!.width + opts.labelPadding
                LegendFormat.BOTTOM -> dh += legend.rect!!.height
                else -> {
                }
            }
            offset(dx, dy, dw, dh)
        }

        fun draw(g: Graphics2D) {
            drawPlotArea(g)
            drawGrid(g)
            drawAxes(g)
            drawLegend(g)
            // if check needed that content is inside padding
            //g.setColor(Color.GRAY);
            //g.drawRect(boundRect.x + opts.padding, boundRect.y + opts.padding, boundRect.width - opts.padding * 2, boundRect.height - opts.padding * 2);
        }

        private fun drawPlotArea(g: Graphics2D) {
            g.color = opts.foregroundColor
            g.drawRect(plotBorderRect.x, plotBorderRect.y, plotBorderRect.width, plotBorderRect.height)
            g.font = opts.titleFont
            drawLabel(g, opts.title, plotBorderRect.x + toInt(plotBorderRect.getWidth() / 2), opts.padding, HorizAlign.CENTER, VertAlign.TOP)
        }

        private fun drawGrid(g: Graphics2D) {
            val stroke = g.stroke
            g.stroke = opts.gridStroke
            g.color = opts.gridColor
            val leftX = plotBorderRect.x + 1
            val rightX = plotBorderRect.x + plotBorderRect.width - 1
            val topY = plotBorderRect.y + 1
            val bottomY = plotBorderRect.y + plotBorderRect.height - 1
            for (i in 0 until opts.grids.x + 1) {
                val x = toInt(plotRect.x + plotRect.getWidth() / opts.grids.x * i)
                g.drawLine(x, topY, x, bottomY)
            }
            for (i in 0 until opts.grids.y + 1) {
                val y = toInt(plotRect.y + plotRect.getHeight() / opts.grids.y * i)
                g.drawLine(leftX, y, rightX, y)
            }
            g.stroke = stroke
        }

        private fun calcAxes(g: Graphics2D) {
            val xAxis = if (xAxes.isEmpty()) Axis("", null) else xAxes.values.iterator().next()
            val yAxis = if (yAxes.isEmpty()) Axis("", null) else yAxes.values.iterator().next()
            var xCount = 0
            var yCount = 0
            for (series in dataSeriesMap.values) {
                if (series.opts!!.xAxis == null) {
                    series.opts!!.xAxis = xAxis
                    xCount++
                }
                if (series.opts!!.yAxis == null) {
                    series.opts!!.yAxis = yAxis
                    yCount++
                }
                series.addAxesToName()
            }
            if (xAxes.isEmpty() && xCount > 0) xAxes["x"] = xAxis
            if (yAxes.isEmpty() && yCount > 0) yAxes["y"] = yAxis
        }

        private fun calcAxisLabels(g: Graphics2D, isX: Boolean) {
            val fm = g.fontMetrics
            var rect: Rectangle2D? = null
            var w = 0.0
            var h = 0.0
            val axes: Map<String?, Axis> = if (isX) xAxes else yAxes
            val grids = if (isX) opts.grids.x else opts.grids.y
            for ((_, axis) in axes) {
                axis.labels = arrayOfNulls(grids + 1)
                axis.labelRect = fm.getStringBounds("", g)
                val xStep = axis.opts.range!!.diff / grids
                for (j in 0 until grids + 1) {
                    axis!!.labels?.set(j, formatDouble(axis.opts.range!!.min1 + xStep * j, axis.opts.format))
                    rect = fm.getStringBounds(axis!!.labels?.get(j), g)
                    if (rect.width > w) w = rect.width
                    if (rect.height > h) h = rect.height
                }
                axis.labelRect!!.setRect(0.0, 0.0, w, h)
            }
        }

        private fun calcRange(isX: Boolean) {
            for (series in dataSeriesMap.values) {
                val axis = if (isX) series.opts!!.xAxis else series.opts!!.yAxis
                if (axis!!.opts.dynamicRange) {
                    val range = if (isX) series.xRange() else series.yRange()
                    if (axis.opts.range == null) axis.opts.range = range else {
                        if (range.max1 > axis.opts.range!!.max1) axis.opts.range!!.setMax(range.max1)
                        if (range.min1 < axis.opts.range!!.min1) axis.opts.range!!.setMin(range.min1)
                    }
                }
            }
            val axes = if (isX) xAxes else yAxes
            val it = axes.values.iterator()
            while (it.hasNext()) {
                val axis = it.next()
                if (axis.opts.range == null) it.remove()
            }
        }

        private fun drawAxes(g: Graphics2D) {
            g.font = opts.labelFont
            g.color = opts.foregroundColor
            val leftXPadded = plotBorderRect.x - opts.labelPadding
            val rightX = plotBorderRect.x + plotBorderRect.width
            val bottomY = plotBorderRect.y + plotBorderRect.height
            val bottomYPadded = bottomY + opts.labelPadding
            var axisOffset = 0
            for ((_, axis) in xAxes) {
                val xStep = axis.opts.range!!.diff / opts.grids.x
                drawLabel(g, axis.name, rightX + opts.labelPadding, bottomY + axisOffset, HorizAlign.LEFT, VertAlign.CENTER)
                g.drawLine(plotRect.x, bottomY + axisOffset, plotRect.x + plotRect.width, bottomY + axisOffset)
                for (j in 0 until opts.grids.x + 1) {
                    val x = toInt(plotRect.x + plotRect.getWidth() / opts.grids.x * j)
                    drawLabel(g, formatDouble(axis.opts.range!!.min1 + xStep * j, axis.opts.format), x, bottomYPadded + axisOffset, HorizAlign.CENTER, VertAlign.TOP)
                    g.drawLine(x, bottomY + axisOffset, x, bottomY + opts.tickSize + axisOffset)
                }
                axisOffset += toInt(axis.labelRect!!.height + opts.labelPadding * 2)
            }
            axisOffset = 0
            for ((_, axis) in yAxes) {
                val yStep = axis.opts.range!!.diff / opts.grids.y
                drawLabel(g, axis.name, leftXPadded - axisOffset, plotBorderRect.y - toInt(axis.labelRect!!.height + opts.labelPadding), HorizAlign.RIGHT, VertAlign.CENTER)
                g.drawLine(plotBorderRect.x - axisOffset, plotRect.y + plotRect.height, plotBorderRect.x - axisOffset, plotRect.y)
                for (j in 0 until opts.grids.y + 1) {
                    val y = toInt(plotRect.y + plotRect.getHeight() / opts.grids.y * j)
                    drawLabel(g, formatDouble(axis.opts.range!!.max1 - yStep * j, axis.opts.format), leftXPadded - axisOffset, y, HorizAlign.RIGHT, VertAlign.CENTER)
                    g.drawLine(plotBorderRect.x - axisOffset, y, plotBorderRect.x - opts.tickSize - axisOffset, y)
                }
                axisOffset += toInt(axis.labelRect!!.width + opts.labelPadding * 2)
            }
        }

        private fun calcLegend(g: Graphics2D) {
            legend.rect = Rectangle(0, 0)
            if (opts.legend == LegendFormat.NONE) return
            val size = dataSeriesMap.size
            if (size == 0) return
            val fm = g.fontMetrics
            val it: Iterator<DataSeries> = dataSeriesMap.values.iterator()
            legend.labelRect = fm.getStringBounds(it.next().nameWithAxes, g)
            var legendSignSize = opts.defaultLegendSignSize
            while (it.hasNext()) {
                val series = it.next()
                val rect = fm.getStringBounds(series.nameWithAxes, g)
                if (rect.width > legend.labelRect!!.getWidth()) legend.labelRect!!.setRect(0.0, 0.0, rect.width, legend.labelRect!!.getHeight())
                if (rect.height > legend.labelRect!!.getHeight()) legend.labelRect!!.setRect(0.0, 0.0, legend.labelRect!!.getWidth(), rect.height)
                when (series.opts!!.marker) {
                    Marker.CIRCLE, Marker.SQUARE -> if (series.opts!!.markerSize + opts.defaultLegendSignSize > legendSignSize) legendSignSize = series.opts!!.markerSize + opts.defaultLegendSignSize
                    //Marker.DIAMOND -> if (series.getDiagMarkerSize() + opts.defaultLegendSignSize > legendSignSize) legendSignSize = series.getDiagMarkerSize() + opts.defaultLegendSignSize
                    else -> {
                    }
                }
            }
            opts.legendSignSize = legendSignSize
            legend.entryWidth = legendSignSize + opts.labelPadding + toInt(legend.labelRect!!.getWidth())
            legend.entryWidthPadded = legend.entryWidth + opts.labelPadding
            when (opts.legend) {
                LegendFormat.TOP, LegendFormat.BOTTOM -> {
                    legend.entryCount = Math.floor((plotBorderRect.width - opts.labelPadding).toDouble() / legend.entryWidthPadded).toInt()
                    legend.xCount = if (size <= legend.entryCount) size else legend.entryCount
                    legend.yCount = if (size <= legend.entryCount) 1 else Math.ceil(size.toDouble() / legend.entryCount).toInt()
                    legend.rect!!.width = opts.labelPadding + legend.xCount * legend.entryWidthPadded
                    legend.rect!!.height = opts.labelPadding + toInt(legend.yCount * (opts.labelPadding + legend.labelRect!!.getHeight()))
                    legend.rect!!.x = plotBorderRect.x + (plotBorderRect.width - legend.rect!!.width) / 2
                    if (opts.legend == LegendFormat.TOP) legend.rect!!.y = plotBorderRect.y else legend.rect!!.y = boundRect.height - legend.rect!!.height - opts.padding
                }
                LegendFormat.RIGHT -> {
                    legend.rect!!.width = opts.labelPadding * 3 + legendSignSize + toInt(legend.labelRect!!.getWidth())
                    legend.rect!!.height = opts.labelPadding * (size + 1) + toInt(legend.labelRect!!.getHeight() * size)
                    legend.rect!!.x = boundRect.width - legend.rect!!.width - opts.padding
                    legend.rect!!.y = plotBorderRect.y + plotBorderRect.height / 2 - legend.rect!!.height / 2
                }
                else -> {
                }
            }
        }



        private fun drawLegend(g: Graphics2D) {
            if (opts.legend == LegendFormat.NONE) return
            g.drawRect(legend.rect!!.x, legend.rect!!.y, legend.rect!!.width, legend.rect!!.height)
            val labelHeight = toInt(legend.labelRect!!.height)
            var x = legend.rect!!.x + opts.labelPadding
            var y = legend.rect!!.y + opts.labelPadding + labelHeight / 2
            when (opts.legend) {
                LegendFormat.TOP, LegendFormat.BOTTOM -> {
                    var i = 0
                    for (series in dataSeriesMap.values) {
                        drawLegendEntry(g, series, x, y)
                        x += legend.entryWidthPadded
                        if ((i + 1) % legend.xCount == 0) {
                            x = legend.rect!!.x + opts.labelPadding
                            y += opts.labelPadding + labelHeight
                        }
                        i++
                    }
                }
                LegendFormat.RIGHT -> for (series in dataSeriesMap.values) {
                    drawLegendEntry(g, series, x, y)
                    y += opts.labelPadding + labelHeight
                }
                else -> {
                }
            }
        }

        private fun drawLegendEntry(g: Graphics2D, series: DataSeries, x: Int, y: Int) {
            series.fillArea(g, x, y, x + opts.legendSignSize, y, y + opts.legendSignSize / 2)
            series.drawLine(g, x, y, x + opts.legendSignSize, y)
            series.drawMarker(g, x + opts.legendSignSize / 2, y, x, y + opts.legendSignSize / 2)
            g.color = opts.foregroundColor
            drawLabel(g, series.nameWithAxes, x + opts.legendSignSize + opts.labelPadding, y, HorizAlign.LEFT, VertAlign.CENTER)
        }

        init {
            clear()
        }
    }

    class Range {
        var min1: Double
        var max1: Double
        var diff: Double

        constructor(min: Double, max: Double) {
            this.min1 = min
            this.max1 = max
            diff = max - min
        }

        constructor(range: Range) {
            min1 = range.min1
            max1 = range.max1
            diff = max1 - min1
        }

        fun setMin(min: Double) {
            this.min1 = min
            diff = max1 - min
        }

        fun setMax(max: Double) {
            this.max1 = max
            diff = max - min1
        }

        override fun toString(): String {
            return "Range [min=$min1, max=$max1]"
        }
    }

    class AxisOptions {
        var format = AxisFormat.NUMBER
        var dynamicRange = true
        var range: Range? = null
        fun format(format: AxisFormat): AxisOptions {
            this.format = format
            return this
        }

        fun range(min: Double, max: Double): AxisOptions {
            range = Range(min, max)
            dynamicRange = false
            return this
        }
    }

    inner class Axis(val name: String, opts: AxisOptions?) {
        var opts = AxisOptions()
        var labelRect: Rectangle2D? = null
        var labels: Array<String?>? = null
        override fun toString(): String {
            return "Axis [name=$name, opts=$opts]"
        }

        init {
            if (opts != null) this.opts = opts
        }
    }

    class DataSeriesOptions {
        var seriesColor = Color.BLUE
        var line = Line.SOLID
        var lineWidth = 2
        var lineDash = floatArrayOf(3.0f, 3.0f)
        var marker = Marker.NONE
        var markerSize = 10
        var markerColor = Color.WHITE
        var areaColor: Color? = null
        private var xAxisName: String? = null
        private var yAxisName: String? = null
         var xAxis: Axis? = null
         var yAxis: Axis? = null
        fun color(seriesColor: Color): DataSeriesOptions {
            this.seriesColor = seriesColor
            return this
        }

        fun line(line: Line): DataSeriesOptions {
            this.line = line
            return this
        }

        fun lineWidth(width: Int): DataSeriesOptions {
            lineWidth = width
            return this
        }

        fun lineDash(dash: FloatArray): DataSeriesOptions {
            lineDash = dash
            return this
        }

        fun marker(marker: Marker): DataSeriesOptions {
            this.marker = marker
            return this
        }

        fun markerSize(markerSize: Int): DataSeriesOptions {
            this.markerSize = markerSize
            return this
        }

        fun markerColor(color: Color): DataSeriesOptions {
            markerColor = color
            return this
        }

        fun areaColor(color: Color?): DataSeriesOptions {
            areaColor = color
            return this
        }

        fun xAxis(name: String?): DataSeriesOptions {
            xAxisName = name
            return this
        }

        fun yAxis(name: String?): DataSeriesOptions {
            yAxisName = name
            return this
        }

        fun setPlot(plot: Plot?) {
            if (plot != null) xAxis = plot.xAxes[xAxisName]
            if (plot != null) yAxis = plot.yAxes[yAxisName]
        }
    }

    class Data {
        private var x1: DoubleArray? = null
        private var y1: DoubleArray? = null
        private var x2: MutableList<Double>? = null
        private var y2: MutableList<Double>? = null
        fun xy(x: DoubleArray?, y: DoubleArray?): Data {
            x1 = x
            y1 = y
            return this
        }

        fun xy(x: Double, y: Double): Data {
            if (x2 == null || y2 == null) {
                x2 = ArrayList(10)
                y2 = ArrayList(10)
            }
            x2!!.add(x)
            y2!!.add(y)
            return this
        }

        fun xy(x: MutableList<Double>?, y: MutableList<Double>?): Data {
            x2 = x
            y2 = y
            return this
        }

        fun size(): Int {
            if (x1 != null) return x1!!.size
            return if (x2 != null) x2!!.size else 0
        }

        fun x(i: Int): Double {
            if (x1 != null) return x1!![i]
            return if (x2 != null) x2!![i] else 0.0
        }

        fun y(i: Int): Double {
            if (y1 != null) return y1!![i]
            return if (y2 != null) y2!![i] else 0.0
        }
    }

    inner class DataSeries(name: String, data: Data?, opts: DataSeriesOptions?) {
        private val name: String
        var nameWithAxes: String? = null
        var opts: DataSeriesOptions? = DataSeriesOptions()
        var data: Data?
        fun clear() {}
        fun addAxesToName() {
            nameWithAxes = name + " (" + opts!!.yAxis!!.name + "/" + opts!!.xAxis!!.name + ")"
        }

        fun xRange(): Range {
            var range = Range(0.0, 0.0)
            if (data != null && data!!.size() > 0) {
                range = Range(data!!.x(0), data!!.x(0))
                for (i in 1 until data!!.size()) {
                    if (data!!.x(i) > range.max1) range.setMax(data!!.x(i))
                    if (data!!.x(i) < range.min1) range.setMin(data!!.x(i))
                }
            }
            return range
        }

        fun yRange(): Range {
            var range = Range(0.0, 0.0)
            if (data != null && data!!.size() > 0) {
                range = Range(data!!.y(0), data!!.y(0))
                for (i in 1 until data!!.size()) {
                    if (data!!.y(i) > range.max1) range.setMax(data!!.y(i))
                    if (data!!.y(i) < range.min1) range.setMin(data!!.y(i))
                }
            }
            return range
        }

        fun draw(g: Graphics2D) {
            g.clip = plotArea.plotClipRect
            if (data != null) {
                var x1 = 0.0
                var y1 = 0.0
                val size = data!!.size()
                if (opts!!.line != Line.NONE) for (j in 0 until size) {
                    val x2 = x2x(data!!.x(j), opts!!.xAxis!!.opts.range, plotArea.xPlotRange)
                    val y2 = y2y(data!!.y(j), opts!!.yAxis!!.opts.range, plotArea.yPlotRange)
                    var ix1 = toInt(x1)
                    var iy1 = toInt(y1)
                    val ix2 = toInt(x2)
                    val iy2 = toInt(y2)
                    val iy3 = plotArea.plotRect.y + plotArea.plotRect.height
                    // special case for the case when only the first point present
                    if (size == 1) {
                        ix1 = ix2
                        iy1 = iy2
                    }
                    if (j != 0 || size == 1) {
                        fillArea(g, ix1, iy1, ix2, iy2, iy3)
                        drawLine(g, ix1, iy1, ix2, iy2)
                    }
                    x1 = x2
                    y1 = y2
                }
                val halfMarkerSize = opts!!.markerSize / 2
                val halfDiagMarkerSize = diagMarkerSize / 2
                g.stroke = BasicStroke(2.0F)
                if (opts!!.marker != Marker.NONE) for (j in 0 until size) {
                    val x2 = x2x(data!!.x(j), opts!!.xAxis!!.opts.range, plotArea.xPlotRange)
                    val y2 = y2y(data!!.y(j), opts!!.yAxis!!.opts.range, plotArea.yPlotRange)
                    drawMarker(g, halfMarkerSize, halfDiagMarkerSize, x2, y2,
                            plotArea.plotRect.x.toDouble(), plotArea.plotRect.y + plotArea.plotRect.height.toDouble())
                }
            }
        }

        private val diagMarkerSize: Int
            private get() = Math.round(Math.sqrt(2 * opts!!.markerSize * opts!!.markerSize.toDouble())).toInt()

        fun fillArea(g: Graphics2D, ix1: Int, iy1: Int, ix2: Int, iy2: Int, iy3: Int) {
            if (opts!!.areaColor != null) {
                g.color = opts!!.areaColor
                g.fill(Polygon(intArrayOf(ix1, ix2, ix2, ix1), intArrayOf(iy1, iy2, iy3, iy3),
                        4))
                g.color = opts!!.seriesColor
            }
        }

        fun drawLine(g: Graphics2D, ix1: Int, iy1: Int, ix2: Int, iy2: Int) {
            if (opts!!.line != Line.NONE) {
                g.color = opts!!.seriesColor
                setStroke(g)
                g.drawLine(ix1, iy1, ix2, iy2)
            }
        }

        private fun setStroke(g: Graphics2D) {
            when (opts!!.line) {
                Line.SOLID -> g.stroke = BasicStroke(opts!!.lineWidth.toFloat())
                Line.DASHED -> g.stroke = BasicStroke(opts!!.lineWidth.toFloat(), BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 10.0f, opts!!.lineDash, 0.0f)
                else -> {
                }
            }
        }

        fun drawMarker(g: Graphics2D, x2: Int, y2: Int, x3: Int, y3: Int) {
            val halfMarkerSize = opts!!.markerSize / 2
            val halfDiagMarkerSize = diagMarkerSize / 2
            g.stroke = BasicStroke(2.0F)
            drawMarker(g, halfMarkerSize, halfDiagMarkerSize, x2.toDouble(), y2.toDouble(), x3.toDouble(), y3.toDouble())
        }

        private fun drawMarker(g: Graphics2D, halfMarkerSize: Int, halfDiagMarkerSize: Int, x2: Double, y2: Double, x3: Double, y3: Double) {
            when (opts!!.marker) {
                Marker.CIRCLE -> {
                    g.color = opts!!.markerColor
                    g.fillOval(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts!!.markerSize, opts!!.markerSize)
                    g.color = opts!!.seriesColor
                    g.drawOval(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts!!.markerSize, opts!!.markerSize)
                }
                Marker.SQUARE -> {
                    g.color = opts!!.markerColor
                    g.fillRect(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts!!.markerSize, opts!!.markerSize)
                    g.color = opts!!.seriesColor
                    g.drawRect(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts!!.markerSize, opts!!.markerSize)
                }
                Marker.DIAMOND -> {
                    val xpts = intArrayOf(toInt(x2), toInt(x2 + halfDiagMarkerSize), toInt(x2), toInt(x2 - halfDiagMarkerSize))
                    val ypts = intArrayOf(toInt(y2 - halfDiagMarkerSize), toInt(y2), toInt(y2 + halfDiagMarkerSize), toInt(y2))
                    g.color = opts!!.markerColor
                    g.fillPolygon(xpts, ypts, 4)
                    g.color = opts!!.seriesColor
                    g.drawPolygon(xpts, ypts, 4)
                }
                Marker.COLUMN -> {
                    g.color = opts!!.markerColor
                    g.fillRect(toInt(x2), toInt(y2), opts!!.markerSize, toInt(y3 - y2))
                    g.color = opts!!.seriesColor
                    g.drawRect(toInt(x2), toInt(y2), opts!!.markerSize, toInt(y3 - y2))
                }
                Marker.BAR -> {
                    g.color = opts!!.markerColor
                    g.fillRect(toInt(x3), toInt(y2), toInt(x2 - x3), opts!!.markerSize)
                    g.color = opts!!.seriesColor
                    g.drawRect(toInt(x3), toInt(y2), toInt(x2 - x3), opts!!.markerSize)
                }
                else -> {
                }
            }
        }

        init {
            if (opts != null) this.opts = opts
            this.name = name
            this.data = data
            if (this.data == null) this.data = data()
        }
    }

    companion object {
        fun plot(opts: PlotOptions?): Plot {
            return Plot(opts)
        }

        fun plotOpts(): PlotOptions {
            return PlotOptions()
        }

        fun axisOpts(): AxisOptions {
            return AxisOptions()
        }

        fun seriesOpts(): DataSeriesOptions {
            return DataSeriesOptions()
        }

        fun data(): Data {
            return Data()
        }

        private fun drawLabel(g: Graphics2D, s: String?, x: Int, y: Int, hAlign: HorizAlign, vAlign: VertAlign) {
            var x = x
            var y = y
            val fm = g.fontMetrics
            val rect = fm.getStringBounds(s, g)

            // by default align by left
            x -= if (hAlign == HorizAlign.RIGHT)  rect.width.toInt() else if (hAlign == HorizAlign.CENTER) ( rect.width / 2).toInt() else 0

            // by default align by bottom
            y += if (vAlign == VertAlign.TOP) rect.height.toInt() else if (vAlign == VertAlign.CENTER) ( rect.height / 2).toInt() else 0
            g.drawString(s, x, y)
        }

        fun formatDouble(d: Double, format: AxisFormat?): String {
            return when (format) {
                AxisFormat.TIME_HM -> String.format("%tR", Date(d.toLong()))
                AxisFormat.TIME_HMS -> String.format("%tT", Date(d.toLong()))
                AxisFormat.DATE -> String.format("%tF", Date(d.toLong()))
                AxisFormat.DATETIME_HM -> String.format("%tF %1\$tR", Date(d.toLong()))
                AxisFormat.DATETIME_HMS -> String.format("%tF %1\$tT", Date(d.toLong()))
                AxisFormat.NUMBER_KGM -> formatDoubleAsNumber(d, true)
                AxisFormat.NUMBER_INT -> Integer.toString(d.toInt())
                else -> formatDoubleAsNumber(d, false)
            }
        }

        private fun formatDoubleAsNumber(d: Double, useKGM: Boolean): String {
            return if (useKGM && d > 1000 && d < 1000000000000L) {
                val numbers = longArrayOf(1000L, 1000000L, 1000000000L)
                val suffix = charArrayOf('K', 'M', 'G')
                var i = 0
                var r = 0.0
                for (number in numbers) {
                    r = d / number
                    if (r < 1000) break
                    i++
                }
                if (i == suffix.size) i--
                String.format("%1$,.2f%2\$c", r, suffix[i])
            } else String.format("%1$.3G", d)
        }

        private fun x2x(x: Double, xr1: Range?, xr2: Range): Double {
            return if (xr1!!.diff == 0.0) xr2.min1 + xr2.diff / 2 else xr2.min1 + (x - xr1.min1) / xr1.diff * xr2.diff
        }

        // y axis is reverse in Graphics
        private fun y2y(x: Double, xr1: Range?, xr2: Range): Double {
            return if (xr1!!.diff == 0.0) xr2.min1 + xr2.diff / 2 else xr2.max1 - (x - xr1.min1) / xr1.diff * xr2.diff
        }

        private fun toInt(d: Double): Int {
            return Math.round(d).toInt()
        }
    }

    init {
        if (opts != null) this.opts = opts
        boundRect = Rectangle(0, 0, this.opts.width, this.opts.height)
        plotArea = PlotArea()
    }
}