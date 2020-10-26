package xyz.acrylicstyle.healthChecker.util

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors


@Suppress("UNCHECKED_CAST")
object Util {
    fun allowMethods(vararg methods: String) {
        val methodsField: Field = HttpURLConnection::class.java.getDeclaredField("methods")
        val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(methodsField, methodsField.modifiers and Modifier.FINAL.inv())
        methodsField.isAccessible = true
        val oldMethods = methodsField.get(null) as Array<String>
        val methodsSet: MutableSet<String> = LinkedHashSet(listOf(*oldMethods))
        methodsSet.addAll(listOf(*methods))
        val newMethods = methodsSet.toTypedArray()
        methodsField.set(null, newMethods)
    }

    fun toString(i: InputStream) = BufferedReader(InputStreamReader(i, StandardCharsets.UTF_8)).lines().collect(Collectors.joining())!!
}
