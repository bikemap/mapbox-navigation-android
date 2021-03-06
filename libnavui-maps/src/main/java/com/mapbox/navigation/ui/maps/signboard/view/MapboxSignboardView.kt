package com.mapbox.navigation.ui.maps.signboard.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maps.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.signboard.model.SignboardValue
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import java.io.ByteArrayInputStream

/**
 * Default Signboard View that renders snapshot.
 */
class MapboxSignboardView : AppCompatImageView {

    /**
     *
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : super(context)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @param defStyleAttr Int
     * @constructor
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    /**
     * Invoke to render the signboard based on data or error conditions.
     */
    fun render(result: Expected<SignboardValue, SignboardError>) {
        when (result) {
            is Expected.Success -> {
                val signboard = renderSignboard(result.value)
                visibility = if (signboard != null) {
                    setImageBitmap(signboard)
                    VISIBLE
                } else {
                    setImageBitmap(null)
                    GONE
                }
            }
            is Expected.Failure -> {
                setImageBitmap(null)
                visibility = GONE
            }
        }
    }

    private fun renderSignboard(value: SignboardValue): Bitmap? {
        return when (value.bytes.isEmpty()) {
            true -> null
            else -> {
                val stream = ByteArrayInputStream(value.bytes)
                SvgUtil.renderAsBitmapWithWidth(stream, value.desiredSignboardWidth)
            }
        }
    }
}
