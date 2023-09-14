package com.yunchi.denpendency_inject

import java.sql.Connection
import java.sql.Driver
import java.sql.SQLException
import java.util.*
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object DriverProvider {
    private val drivers = HashMap<KClass<*>, Driver>()

    fun loadService(providers: Stream<KClass<Driver>>){
        providers
            .filter { it !in drivers.keys }
            .forEach {
            drivers[it] = it.createInstance()
        }
    }

    fun openConnection(url: String, name: String?, pwd: String?): Connection{
        val info = Properties()

        if (name != null)
            info["name"] = name
        if (pwd != null)
            info["password"] = pwd

        return drivers
            .values
            .firstNotNullOfOrNull {
                it.connect(url, info)
            }?:
            throw SQLException("No suitable driver found for $url", "08001")

    }
}