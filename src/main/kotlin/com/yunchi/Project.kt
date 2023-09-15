package com.yunchi

import com.yunchi.core.utilities.TimeUnit
import com.yunchi.denpendency_inject.DriverProvider
import com.yunchi.denpendency_inject.loadServices
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ktorm.database.Database
import java.io.File
import java.sql.Connection
import java.sql.Driver
import kotlin.random.Random

object Project{
    val database: Database
        get() = _database
    val config: Config
        get() = _config
    val randGen = Random(System.currentTimeMillis())
    val supportThirdParties = setOf(
        "WeChat"
    )
    val numberTemplate = Regex("[0-9]+")
    val phoneTemplate = Regex("[0-9]{11}")
    val emailTemplate = Regex(
        "^\\w+(?:[-+.]\\w+)*@\\w+(?:[-.]\\w+)*\\.\\w+(?:[-.]\\w+)*\$"
    )


    private lateinit var _config: Config
    private lateinit var _connection: Connection
    private lateinit var _database: Database

    private val configFile: File = fileIfNotExist("./config.json")
    private val driverDir: File = dirIfNotExist("./dbDrivers/")

    init {
        defaultConfig()

        loadConfig()
        loadDrivers()

        makeConnection()

        configAutoClose()
    }

    private fun configAutoClose() {
        Runtime.getRuntime()
            .addShutdownHook(Thread{
                _connection.close()
            })
    }

    private fun defaultConfig(){
        _config = Config(
            "md5",
            "data/",
            Config.VerificationConfig(
                5,
                TimeUnit.MINUTE
            ),
            Config.SMTPConfig(
                "",
                0,
                "",
                "",
                "mail.html"
            ),
            Config.ServerConfig(
                "127.0.0.1",
                8080,
                0,
                0,
                "*"
            ),
            Config.DatabaseConfig(
                "jdbc:sqlite:yunchi.db",
                null,
                null
            ),
            Config.AdminConfig(
                "",
                ""
            ),
            Config.ThirdPartyConfig(
                Config.ThirdPartyConfig.TencentPassword(
                    "", ""
                )
            )
        )

        dirIfNotExist("./data/")
        fileIfNotExist("data/colleges.csv")
    }

    private fun loadConfig(){
        val content = configFile.readText(Charsets.UTF_8)
        if (content.isNotBlank())
            _config = Json.decodeFromString(content)
    }

    private fun loadDrivers(){
        driverDir.listFiles{ file ->
            file.extension == "jar"
        }!!.map{
            loadServices(it, Driver::class, javaClass.classLoader)
        }.forEach(DriverProvider::loadService)
    }

    private fun makeConnection(){
        _database = Database.connect {
            _connection = DriverProvider.openConnection(
                config.databaseConfig.url,
                config.databaseConfig.name,
                config.databaseConfig.password
            )

            _connection.nopClose()
        }
    }

    @Serializable
    data class Config(
        val hashMethod: String,
        val resource: String,
        val verification: VerificationConfig,
        val smtp: SMTPConfig,
        val server: ServerConfig,
        val databaseConfig: DatabaseConfig,
        val adminConfig: AdminConfig,
        val thirdParty: ThirdPartyConfig
    ){
        @Serializable
        data class VerificationConfig(
            val expireTime: Long,
            val timeUnit: TimeUnit
        )

        @Serializable
        data class SMTPConfig(
            val server: String,
            val port: Int,
            val mail: String,
            val password: String,
            val htmlPattern: String
        )

        @Serializable
        data class ServerConfig(
            val baseUrl: String,
            val port: Int,
            val areaId: Int,
            val machineId: Int,
            val friendDomain: String
        )

        @Serializable
        data class DatabaseConfig(
            val url: String,
            val name: String?,
            val password: String?
        )

        @Serializable
        data class AdminConfig(
            val name: String?,
            val password: String?
        )

        @Serializable
        data class ThirdPartyConfig(
            val weChat: TencentPassword
        ){
            @Serializable
            data class TencentPassword(
                val appid: String,
                val appKey: String
            )
        }
    }
}

val Config: Project.Config
    get() = Project.config

fun fileIfNotExist(path: String): File {
    val f = File(path)
    if (! f.exists())
        f.createNewFile()
    return f
}

fun dirIfNotExist(path: String): File {
    val f = File(path)
    if (! f.exists())
        f.mkdirs()
    return f
}

private fun Connection.nopClose(): Connection {
    return object: Connection by this{
        override fun close() {}
    }
}

fun ApplicationResponse.configure(vararg headers: String = arrayOf("*")){
    this.header("Access-Control-Allow-Origin", "*")
    this.header("Access-Control-Allow-Headers", headers.joinToString(
        ", "
    ))
}

fun Random.nextAlNum(): Char{
    return when(nextInt(0, 4)){
        0 -> nextInt('a'.code, 'z'.code).toChar()
        1 -> nextInt('A'.code, 'Z'.code).toChar()
        2 -> nextInt('0'.code, '9'.code).toChar()
        else -> '_'
    }
}