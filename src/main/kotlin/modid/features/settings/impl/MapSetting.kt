package modid.features.settings.impl

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import modid.features.settings.Saving
import modid.features.settings.Setting

/**
 * This setting is only designed to store values as a map, and shouldn't be rendered in the gui.
 *
 * @author Stivais
 */
class MapSetting<K, V, T : MutableMap<K, V>>(
    name: String,
    override val default: T,
    description: String,
    hidden: Boolean = false
) : Setting<T>(name, hidden, description),
    Saving {

    override var value: T = default

    override fun write(): JsonElement {
        return gson.toJsonTree(value)
    }

    override fun read(element: JsonElement?) {
        element?.let {
            val temp = gson.fromJson<T>(it, object : TypeToken<T>() {}.type)
            value.clear()
            value.putAll(temp)
        }
    }
}