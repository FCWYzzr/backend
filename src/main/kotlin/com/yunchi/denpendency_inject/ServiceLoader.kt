package com.yunchi.denpendency_inject

import java.io.File
import java.net.URLClassLoader
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.text.Charsets.UTF_8

fun <T: Any, R: T> loadServices(jarLib: File, type: KClass<T>, cl: ClassLoader): Stream<KClass<R>> {
    val loader = URLClassLoader(
        arrayOf(jarLib.toURI().toURL()),
        cl
    )

    val res = loader
        .getResourceAsStream("META-INF/services/${type.qualifiedName}")
        ?: return Stream.empty()

    return res.reader(UTF_8)
        .readLines()
        .stream()
        .map{try {
            loader.loadClass(it)
        }catch (any: Throwable){any.printStackTrace();null}}
        .filter { it != null }
        .map { it!!.kotlin }
        .filter { it.isSubclassOf(type) }
        .map {
            @Suppress("UNCHECKED_CAST")
            it as KClass<R>
        }
}

fun Stream<*>.consume(){
    this.forEach {}
}