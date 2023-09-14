package com.yunchi.core.protocol.orm

import org.ktorm.dsl.Query
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.asIterable
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import org.ktorm.schema.TextSqlType
import org.w3c.dom.Text
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import kotlin.reflect.KClass

fun <E: Enum<E>> BaseTable<*>.enumOf(name: String, factory: (String) -> E): Column<E> {
    return registerColumn(name, EnumSqlType(factory))
}

fun <E> BaseTable<*>.list(
    name: String,
    saver: (E) -> String = { it.toString() },
    loader: (String) -> E,
    ): Column<List<E>> {
    return registerColumn(name, ListSqlType(saver, loader))
}

class EnumSqlType<E: Enum<E>>(
    private val factory: (String) -> E
) : SqlType<E>(Types.LONGVARCHAR, "text") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: E) {
        ps.setString(index, parameter.toString())
    }

    override fun doGetResult(rs: ResultSet, index: Int): E? {
        val name = rs.getString(index) ?: return null
        return factory.invoke(name)
    }
}

class ListSqlType<E>(
    private val saver: (E) -> String,
    private val loader: (String) -> E
): SqlType<List<E>>(Types.LONGVARCHAR, "text"){
    override fun doGetResult(rs: ResultSet, index: Int): List<E>? {
        val text = rs.getString(index) ?: return null
        return text.split(";").map(loader)
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: List<E>) {
        ps.setString(index, parameter.joinToString(
            ";", transform=saver
        ))
    }

}

fun Query.firstOrNull(): QueryRowSet? {
    return asIterable().firstOrNull()
}