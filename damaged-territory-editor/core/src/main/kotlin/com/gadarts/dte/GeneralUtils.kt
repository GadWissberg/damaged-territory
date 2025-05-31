package com.gadarts.dte

import com.badlogic.gdx.utils.Disposable
import java.lang.reflect.Field
import java.util.*

object GeneralUtils {
    fun <T> disposeObject(instance: T, clazz: Class<T>) {
        val fields = clazz.declaredFields
        Arrays.stream(fields).forEach { field: Field ->
            if (Disposable::class.java.isAssignableFrom(field.type)) {
                field.isAccessible = true

                try {
                    val fieldValue = field[instance]
                    if (fieldValue is Disposable) {
                        fieldValue.dispose()
                    }
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}
