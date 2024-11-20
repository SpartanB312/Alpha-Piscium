import java.io.File
import java.math.BigDecimal
import java.util.*
import kotlin.math.pow

class FloatProgression(val start: Double, val endInclusive: Double, val step: Double) : Iterable<Double> {
    override fun iterator(): Iterator<Double> = object : Iterator<Double> {
        private val startBig = BigDecimal.valueOf(start)
        private val endInclusiveBig = BigDecimal.valueOf(endInclusive)
        private val stepBig = BigDecimal.valueOf(step)
        private var index = BigDecimal.ZERO

        override fun hasNext(): Boolean = startBig + stepBig * index <= endInclusiveBig

        override fun next(): Double {
            val value = startBig + stepBig * index
            index += BigDecimal.ONE
            return value.toDouble()
        }
    }
}

infix fun ClosedFloatingPointRange<Double>.step(step: Double): FloatProgression =
    FloatProgression(start, endInclusive, step)

class ScreenItem(val name: String) {
    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenItem) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        val EMPTY = ScreenItem("<empty>")
    }
}

abstract class OptionFactory {
    abstract val scope: Scope

    fun constToggle(name: String, value: Boolean, block: OptionBuilder<Boolean>.() -> Unit = {}): ScreenItem {
        val screenItem = ScreenItem(name)
        scope._addOption(OptionBuilder(name, value, true, emptyList()).apply(block))
        return screenItem
    }

    fun constToggle(
        name: String,
        value: Int,
        range: Iterable<Int>,
        block: OptionBuilder<Int>.() -> Unit = {}
    ): ScreenItem {
        val screenItem = ScreenItem(name)
        scope._addOption(OptionBuilder(name, value, true, range).apply(block))
        return screenItem
    }

    fun constToggle(
        name: String,
        value: Double,
        range: Iterable<Double>,
        block: OptionBuilder<Double>.() -> Unit = {}
    ): ScreenItem {
        val screenItem = ScreenItem(name)
        scope._addOption(OptionBuilder(name, value, true, range).apply(block))
        return screenItem
    }

    fun constSlider(name: String, value: Boolean, block: OptionBuilder<Boolean>.() -> Unit = {}): ScreenItem {
        scope._addSlider(name)
        return constToggle(name, value, block)
    }

    fun constSlider(
        name: String,
        value: Int,
        range: Iterable<Int>,
        block: OptionBuilder<Int>.() -> Unit = {}
    ): ScreenItem {
        scope._addSlider(name)
        return constToggle(name, value, range, block)
    }

    fun constSlider(
        name: String,
        value: Double,
        range: Iterable<Double>,
        block: OptionBuilder<Double>.() -> Unit = {}
    ): ScreenItem {
        scope._addSlider(name)
        return constToggle(name, value, range, block)
    }

    fun toggle(name: String, value: Boolean, block: OptionBuilder<Boolean>.() -> Unit = {}): ScreenItem {
        val screenItem = ScreenItem(name)
        scope._addOption(OptionBuilder(name, value, false, emptyList()).apply(block))
        handleOption(screenItem)
        return screenItem
    }

    fun toggle(name: String, value: Int, range: Iterable<Int>, block: OptionBuilder<Int>.() -> Unit = {}): ScreenItem {
        val screenItem = ScreenItem(name)
        scope._addOption(OptionBuilder(name, value, false, range).apply(block))
        handleOption(screenItem)
        return screenItem
    }

    fun toggle(
        name: String,
        value: Double,
        range: Iterable<Double>,
        block: OptionBuilder<Double>.() -> Unit = {}
    ): ScreenItem {
        val screenItem = ScreenItem(name)
        scope._addOption(OptionBuilder(name, value, false, range).apply(block))
        handleOption(screenItem)
        return screenItem
    }

    fun slider(name: String, value: Boolean, block: OptionBuilder<Boolean>.() -> Unit = {}): ScreenItem {
        scope._addSlider(name)
        return toggle(name, value, block)
    }

    fun slider(
        name: String,
        value: Int,
        range: Iterable<Int>,
        block: OptionBuilder<Int>.() -> Unit = {}
    ): ScreenItem {
        scope._addSlider(name)
        return toggle(name, value, range, block)
    }

    fun slider(
        name: String,
        value: Double,
        range: Iterable<Double>,
        block: OptionBuilder<Double>.() -> Unit = {}
    ): ScreenItem {
        scope._addSlider(name)
        return toggle(name, value, range, block)
    }

    protected open fun handleOption(item: ScreenItem) {}
}


class OptionBuilder<T>(
    val name: String,
    private val value: T,
    private val const: Boolean,
    private val range: Iterable<T>
) {
    private val langBuilders = mutableMapOf<Locale, LangBuilder<T>>()

    fun lang(locale: Locale, block: LangBuilder<T>.() -> Unit) {
        langBuilders.getOrPut(locale) { LangBuilder(name, locale) }.block()
    }

    class LangBuilder<T>(private val optionName: String, private val locale: Locale) {
        var name = ""
            set(value) {
                check(value.isNotEmpty()) { "Name cannot be empty" }; field = value
            }
        var comment = ""
            set(value) {
                check(value.isNotEmpty()) { "Comment cannot be empty" }; field = value
            }

        var prefix = ""
            set(value) {
                check(value.isNotEmpty()) { "Prefix cannot be empty" }; field = value
            }

        var suffix = ""
            set(value) {
                check(value.isNotEmpty()) { "Suffix cannot be empty" }; field = value
            }

        private val valueLabel = mutableMapOf<T, String>()

        infix fun T.value(label: String) {
            check(label.isNotEmpty()) { "Label cannot be empty" }
            valueLabel[this] = label
        }

        fun build(output: Scope.Output) {
            output.writeLang(locale) {
                if (name.isNotEmpty()) appendLine("option.$optionName=$name")
                if (comment.isNotEmpty()) appendLine("option.$optionName.comment=$comment")
                if (prefix.isNotEmpty()) appendLine("option.$optionName.prefix=$prefix")
                if (suffix.isNotEmpty()) appendLine("option.$optionName.suffix=$suffix")
                valueLabel.forEach { (value, label) ->
                    appendLine("value.$optionName.$value=$label")
                }
            }
        }
    }

    fun build(output: Scope.Output) {
        output.writeOption {
            if (value is Boolean) {
                if (const) {
                    appendLine("const bool $name = $value;")
                } else {
                    if (value) {
                        appendLine("#define $name")
                    } else {
                        appendLine("//#define $name")
                    }
                    appendLine("#ifdef $name")
                    appendLine("#endif")
                }
            } else {
                if (const) {
                    when (value) {
                        is Int -> append("const int $name = $value;")
                        is Double -> append("const float $name = $value;")
                        else -> error("Unsupported type")
                    }
                } else {
                    append("#define $name $value")
                }
                range.joinTo(this, " ", " // [", "]")
                appendLine()
            }
        }
        langBuilders.forEach { (_, builder) ->
            builder.build(output)
        }
    }
}

class Scope : OptionFactory() {
    private lateinit var _mainScreen: ScreenBuilder
    private val _screens = mutableSetOf<ScreenBuilder>()
    private val _sliders = mutableSetOf<String>()
    private val _options = mutableSetOf<OptionBuilder<*>>()

    override val scope: Scope
        get() = this

    internal fun _addScreen(screen: ScreenBuilder) {
        check(_screens.add(screen)) { "Screen ${screen.name} already exists" }
    }

    internal fun _addOption(option: OptionBuilder<*>) {
        check(_options.add(option)) { "Option ${option.name} already exists" }
    }

    internal fun _addSlider(name: String) {
        check(_sliders.add(name)) { "Slider $name already exists" }
    }

    fun mainScreen(columns: Int, block: ScreenBuilder.() -> Unit) {
        check(!::_mainScreen.isInitialized) { "Main screen already exists" }
        _mainScreen = ScreenBuilder(this, "", columns)
        _mainScreen.apply(block)
    }

    fun build(baseShadersProperties: File): Output {
        val output = Output(baseShadersProperties)
        output.writeShadersProperties {
            _sliders.joinTo(this, " ", "sliders=")
            appendLine()
        }
        _mainScreen.build(output)
        _screens.forEach { screen ->
            screen.build(output)
        }
        _options.forEach { option ->
            option.build(output)
        }
        return output
    }

    class ScreenBuilder(override val scope: Scope, var name: String, val columns: Int) : OptionFactory() {
        init {
            check(!name.contains(' ')) { "Screen name cannot contain space" }
        }

        private val langBuilders = mutableMapOf<Locale, LangBuilder>()
        private val options = mutableSetOf<OptionBuilder<*>>()
        private val ref = if (name.isEmpty()) "" else ".${this@ScreenBuilder.name}"
        private val items = mutableListOf<ScreenItem>()

        fun lang(locale: Locale, block: LangBuilder.() -> Unit) {
            check(name.isNotEmpty()) { "Main screen cannot have lang" }
            langBuilders.getOrPut(locale) { LangBuilder(ref, locale) }.block()
        }

        fun build(output: Output) {
            langBuilders.forEach { (_, builder) ->
                builder.build(output)
            }
            output.writeShadersProperties {
                appendLine("screen$ref.columns=$columns")
                append("screen$ref=")
                items.joinTo(this, " ")
                appendLine()
            }
        }

        fun item(item: ScreenItem) {
            items.add(item)
        }

        fun screen(name: String, columns: Int, block: ScreenBuilder.() -> Unit) {
            val screen = ScreenBuilder(scope, name, columns)
            scope._addScreen(screen)
            screen.apply(block)
            val screenItem = ScreenItem("[$name]")
            items.add(screenItem)
        }

        fun empty() {
            items.add(ScreenItem.EMPTY)
        }

        override fun handleOption(item: ScreenItem) {
            items.add(item)
        }

        class LangBuilder(private val ref: String, private val locale: Locale) {
            var name = ""
                set(value) {
                    check(value.isNotEmpty()) { "Name cannot be empty" }; field = value
                }

            var comment = ""
                set(value) {
                    check(value.isNotEmpty()) { "Comment cannot be empty" }; field = value
                }

            fun build(output: Output) {
                output.writeLang(locale) {
                    if (name.isNotEmpty()) appendLine("screen$ref=$name")
                    if (comment.isNotEmpty()) appendLine("screen$ref.comment=$comment")
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ScreenBuilder) return false

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    class Output(baseShadersProperties: File) {
        private val _options = StringBuilder()
        private val _lang = mutableMapOf<Locale, StringBuilder>()
        private val _shadersProperties = StringBuilder()

        init {
            _options.appendLine("// $NOTICE")
            _shadersProperties.appendLine("# $NOTICE")
            _shadersProperties.appendLine(baseShadersProperties.readText())
            _shadersProperties.appendLine()
            _shadersProperties.appendLine("# --- Generated Stuff ---")
        }

        fun writeOption(block: Appendable.() -> Unit) {
            _options.block()
        }

        fun writeLang(locale: Locale, block: Appendable.() -> Unit) {
            _lang.getOrPut(locale) {
                StringBuilder().apply {
                    appendLine("# $NOTICE")
                }
            }.block()
        }

        fun writeShadersProperties(block: Appendable.() -> Unit) {
            _shadersProperties.block()
        }

        fun writeOutput(optionGlslFile: File, shaderRoot: File) {
            val langDir = File(shaderRoot, "lang")
            langDir.mkdirs()
            optionGlslFile.writeText(_options.toString())
            _lang.forEach { (language, content) ->
                File(langDir, "${language}.lang").writeText(content.toString())
            }
            File(shaderRoot, "shaders.properties").bufferedWriter().use {
                it.append(_shadersProperties)
            }
        }

        companion object {
            const val NOTICE = "This file is generated by Options.kts. Do not edit this file manually."
        }
    }
}

fun options(baseShadersProperties: File, shaderRootDir: File, optionGlslPath: String, block: Scope.() -> Unit) {
    val absoluteFile = shaderRootDir.absoluteFile
    Scope().apply(block).build(baseShadersProperties).writeOutput(File(absoluteFile, optionGlslPath), absoluteFile)
}

options(File("shaders.properties"), File("../shaders"), "base/Options.glsl") {
    mainScreen(2) {
        screen("WORLD", 2) {
            lang(Locale.US) {
                name = "World Properties"
                comment = "World properties settings. Such as earth, sun, moon, atmosphere, etc."
            }
            screen("SUN", 1) {
                lang(Locale.US) {
                    name = "Sun Properties"
                }
                toggle("SETTING_REAL_SUN_TEMPERATURE", true) {
                    lang(Locale.US) {
                        name = "Use Real Sun Temperature"
                        comment = "Use real sun temperature of 5772 K."
                    }
                }
                slider("SETTING_SUN_TEMPERATURE", 5700, 1000..20000 step 100) {
                    lang(Locale.US) {
                        name = "Sun Temperature"
                        comment = "Temperature of sun in K (kelvin). Affects the color and intensity of sunlight."
                        suffix = " K"
                    }
                }
                slider("SETTING_SUN_RADIUS", 1.0, (-7..10).map { 2.0.pow(it) }) {
                    lang(Locale.US) {
                        name = "Sun Radius"
                        comment = "Radius of sun relative to real sun radius of 696342 km."
                        suffix = " R"
                    }
                }
                slider("SETTING_SUN_DISTANCE", 1.0, (-7..10).map { 2.0.pow(it) }) {
                    lang(Locale.US) {
                        name = "Sun Distance"
                        comment =
                            "Distance of sun in AU (astronomical units), which is relative to real sun distance of 149.6 million km."
                        suffix = " AU"
                    }
                }
            }
            screen("ATMOSPHERE", 1) {
                lang(Locale.US) {
                    name = "Atmosphere Properties"
                }
                slider("SETTING_ATM_ALT_SCALE", 1000, listOf(1, 10, 100).flatMap { 1 * it..10 * it step it } + 1000) {
                    lang(Locale.US) {
                        name = "Atmosphere Altitude Scale"
                        comment = "Value of 1 means 1 block = 1 km, value of 10 means 10 blocks = 1 km, and so on."
                    }
                }
                slider("SETTING_ATM_D_SCALE", 1000, listOf(1, 10, 100).flatMap { 1 * it..10 * it step it } + 1000) {
                    lang(Locale.US) {
                        name = "Atmosphere Distance Scale"
                        comment = "Value of 1 means 1 block = 1 km, value of 10 means 10 blocks = 1 km, and so on."
                    }
                }
            }
        }
        screen("LIGHTING", 2) {
            lang(Locale.US) {
                name = "Lighting"
            }
            screen("SHADOW", 2) {
                lang(Locale.US) {
                    name = "Shadow"
                }
                constSlider("shadowMapResolution", 2048, listOf(1024, 2048, 3072, 4096)) {
                    lang(Locale.US) {
                        name = "Shadow Map Resolution"
                    }
                }
                constSlider("shadowDistance", 192.0, listOf(64.0, 128.0, 192.0, 256.0, 384.0, 512.0)) {
                    lang(Locale.US) {
                        name = "Shadow Render Distance"
                        64.0 value "4 chunks"
                        128.0 value "8 chunks"
                        192.0 value "12 chunks"
                        256.0 value "16 chunks"
                        384.0 value "24 chunks"
                        512.0 value "32 chunks"
                    }
                }
                screen("RTWSM", 1) {
                    lang(Locale.US) {
                        name = "RTWSM"
                        comment = "Rectilinear Texture Warping Shadow Mapping settings"
                    }
                    slider("SETTING_RTWSM_IMAP_SIZE", 1024, listOf(256, 512, 1024)) {
                        lang(Locale.US) {
                            name = "Importance Map Resolution"
                        }
                    }
                    empty()
                    toggle("SETTING_RTWSM_F", true) {
                        lang(Locale.US) {
                            name = "Forward Importance Analysis"
                        }
                    }
                    slider("SETTING_RTWSM_F_BASE", 0.4, 0.0..1.0 step 0.05) {
                        lang(Locale.US) {
                            name = "Forward Base Value"
                        }
                    }
                    slider("SETTING_RTWSM_F_MIN", 8, 0..20) {
                        lang(Locale.US) {
                            name = "Forward Min Value"
                            comment =
                                "Minimum importance value for forward importance analysis. The actual minimum value is calculated as 2^-x."
                        }
                    }
                    slider("SETTING_RTWSM_F_D", 1024, listOf(0) + (0..16).map { 1 shl it }) {
                        lang(Locale.US) {
                            name = "Forward Distance Function"
                        }
                    }
                    empty()
                    toggle("SETTING_RTWSM_B", true) {
                        lang(Locale.US) {
                            name = "Backward Importance Analysis"
                        }
                    }
                    slider("SETTING_RTWSM_B_BASE", 1.0, 0.1..1.0 step 0.05) {
                        lang(Locale.US) {
                            name = "Backward Base Value"
                        }
                    }
                    slider("SETTING_RTWSM_B_MIN", 12, 0..20) {
                        lang(Locale.US) {
                            name = "Backward Min Value"
                            comment =
                                "Minimum importance value for backward importance analysis. The actual minimum value is calculated as 2^-x."
                        }
                    }
                    slider("SETTING_RTWSM_B_D", 16, listOf(0) + (0..10).map { 2 shl it }) {
                        lang(Locale.US) {
                            name = "Backward Distance Function"
                        }
                    }
                    slider("SETTING_RTWSM_B_SN", 4.0, 0.0..10.0 step 0.5) {
                        lang(Locale.US) {
                            name = "Backward Surface Normal Function"
                        }
                    }
                    slider("SETTING_RTWSM_B_SE", 0.1, 0.0..1.0 step 0.05) {
                        lang(Locale.US) {
                            name = "Backward Shadow Edge Function"
                        }
                    }
                }
                screen("PCSS", 1) {
                    lang(Locale.US) {
                        name = "Soft Shadows"
                        comment = "Soft Shadows settings"
                    }
                    slider("SETTING_PCSS_BPF", 1.0, 0.0..10.0 step 0.5) {
                        lang(Locale.US) {
                            name = "Base Penumbra Factor"
                        }
                    }
                    slider("SETTING_PCSS_VPF", 1.0, 0.0..2.0 step 0.1) {
                        lang(Locale.US) {
                            name = "Variable Penumbra Factor"
                            comment =
                                "The penumbra factor is multiplied by the sun angular radius to determine the penumbra size. Noted that the sun angular radius is affected by the sun radius and distance settings."
                        }
                    }
                    slider("SETTING_PCSS_SAMPLE_COUNT", 8, listOf(1, 2, 4, 8, 16, 32, 64)) {
                        lang(Locale.US) {
                            name = "Sample Count"
                        }
                    }
                    slider("SETTING_PCSS_BLOCKER_SEARCH_COUNT", 4, listOf(1, 2, 4, 8, 16)) {
                        lang(Locale.US) {
                            name = "Blocker Search Count"
                        }
                    }
                    slider("SETTING_PCSS_BLOCKER_SEARCH_LOD", 4, 0..8) {
                        lang(Locale.US) {
                            name = "Blocker Search LOD"
                        }
                    }
                }
                empty()
                empty()
                screen("BLOCKLIGHT", 1) {
                    lang(Locale.US) {
                        name = "Block Light Source"
                    }
                    slider("SETTING_FIRE_TEMPERATURE", 1900, 1000..20000 step 100) {
                        lang(Locale.US) {
                            name = "Fire Temperature"
                            comment =
                                "Temperature of fire in K (kelvin). The default value 1900 K is based on real life average."
                        }
                    }
                    slider("SETTING_LAVA_TEMPERATURE", 1200, 1000..20000 step 100) {
                        lang(Locale.US) {
                            name = "Lava Temperature"
                            comment =
                                "Temperature of lava in K (kelvin). The default value 1200 K is based on real life average."
                        }
                    }
                }
            }
        }
        screen("POSTFX", 1) {
            lang(Locale.US) {
                name = "Post Processing"
            }
            screen("EXPOSURE", 1) {
                lang(Locale.US) {
                    name = "Exposure"
                }
                toggle("SETTING_EXPOSURE_MANUAL", false) {
                    lang(Locale.US) {
                        name = "Manual Exposure"
                    }
                }
                slider("SETTING_EXPOSURE_MANUAL_VALUE", -2.5, -10.0..10.0 step 0.1) {
                    lang(Locale.US) {
                        name = "Manual Exposure Value"
                    }
                }
                empty()
                slider("SETTING_EXPOSURE_MAX_EXP", 2.0, 0.1..10.0 step 0.1) {
                    lang(Locale.US) {
                        name = "Auto Exposure Max"
                    }
                }
                slider("SETTING_EXPOSURE_AVG_LUMA_MIX", 0.5, 0.0..10.0 step 0.1) {
                    lang(Locale.US) {
                        name = "Average Luminance Weight"
                        comment = "Weight of average luminance AE in the final exposure value."
                    }
                }
                slider("SETTING_EXPOSURE_AVG_LUMA_TIME", 3.0, 0.0..10.0 step 0.5) {
                    lang(Locale.US) {
                        name = "Average Luminance AE Time"
                        comment = "Time constant for average luminance AE."
                    }
                }
                slider("SETTING_EXPOSURE_AVG_LUMA_TARGET", 0.25, 0.0..1.0 step 0.01) {
                    lang(Locale.US) {
                        name = "Average Luminance Target"
                        comment = "Target average luminance value for average luminance EXPOSURE."
                    }
                }
                empty()
                slider("SETTING_EXPOSURE_TOP_BIN_MIX", 0.0, 0.0..1.0 step 0.1) {
                    lang(Locale.US) {
                        name = "Top Bin Weight"
                        comment = "Weight of top bin AE in the final exposure value."
                    }
                }
                slider("SETTING_EXPOSURE_TOP_BIN_TIME", 1.5, 0.0..10.0 step 0.5) {
                    lang(Locale.US) {
                        name = "Top Bin AE Time"
                        comment = "Time constant for top bin aE."
                    }
                }
                slider("SETTING_EXPOSURE_TOP_BIN_PERCENT", 2.0, 0.1..10.0 step 0.1) {
                    lang(Locale.US) {
                        name = "Top Bin %"
                        comment =
                            "Adjusting exposure to keep the specified percentage of pixels in the top bin of histogram."
                    }
                }
            }
            screen("TONEMAP", 1) {
                lang(Locale.US) {
                    name = "Tone Mapping"
                }
                slider("SETTING_TONEMAP_OUTPUT_GAMMA", 2.2, 0.05..4.0 step 0.05) {
                    lang(Locale.US) {
                        name = "Output Gamma"
                    }
                }
            }
        }
        screen("DEBUG", 1) {
            lang(Locale.US) {
                name = "Debug"
            }
            toggle("SETTING_DEBUG_RTWSM", false)
            toggle("SETTING_DEBUG_ATMOSPHERE", false)
        }
    }
}