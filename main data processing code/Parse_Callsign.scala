// Databricks notebook source
// DBTITLE 1,Extract and generate a mapping table of Icao to Callsign
import org.opensky.libadsb._
import org.opensky.libadsb.msgs._

// COMMAND ----------

object Mode extends Serializable{
  def checkFormat(rawMessage:String):Boolean={
//     val decoder=new ModeSDecoder()
    try{
      val msg=decoder.decode(rawMessage) //判断格式是否正确
      if(tools.isZero(msg.getParity()) || msg.checkParity()){ //判断校验是否正确，以及 DownLinkFormat 的值
        return true
      }else{
        return false
      }
    }catch{
      case _:Throwable => false
    }
  }
  val CheckFormat=udf((rawMessage:String)=>checkFormat(rawMessage))
  
  def getCallsign(rawMessage:String)={
//     val decoder=new ModeSDecoder()
    val msg:ModeSReply=decoder.decode(rawMessage)
    
    msg.getType().toString() match{
      case "ADSB_IDENTIFICATION"=>
        val ident:IdentificationMsg=msg.asInstanceOf[IdentificationMsg]
        (new String(ident.getIdentity())).substring(0,3)
      case _=>null
    }
  }
  val Callsign=udf((rawMessage:String)=>getCallsign(rawMessage))
  
  
  
  def getDownlinkFormat(rawMessage:String) = {
    try{
      val result = Decoder.genericDecoder(rawMessage).getDownlinkFormat()
      result.toString()
    }catch{
      case _:Throwable=>null
    }

  }
  val DownlinkFormat=udf((rawMessage:String)=>getDownlinkFormat(rawMessage))
  
  @transient private lazy val decoder=new ModeSDecoder()

  def getPos(rawMessage:String,timeStamp:Double):(Double,Double,Double)={
    val msg:ModeSReply=decoder.decode(rawMessage)
    val m = msg.getType().toString()
    if (m == "ADSB_AIRBORN_POSITION_V0" || m == "ADSB_AIRBORN_POSITION_V1" || m =="ADSB_AIRBORN_POSITION_V2") {
        val ap0 = msg.asInstanceOf[AirbornePositionV0Msg];
        val c0:Position=decoder.decodePosition(timeStamp.toLong,ap0,null)
        if(c0!=null){
          return (c0.getLongitude(), c0.getLatitude(),c0.getAltitude())
        }
    }
    null
//    (0.0,0.0,0.0)
  }
  val Pos=udf((rawMessage:String,timeStamp:Double)=>getPos(rawMessage,timeStamp))
  
  def getIcao(raw:String): String = {
     try{
      val icao24 = Decoder.genericDecoder(raw).getIcao24()
      tools.toHexString(icao24)
    } catch {
      case _: Throwable => null
    }
  }
  val Icao24 = udf[String, String](getIcao _)
}



// COMMAND ----------

// val df = spark.read.format("com.databricks.spark.avro")
//                 .load("/mnt/lsde/opensky/2016/*/*.avro")
// val df = spark.read.option("header",true)
//                 .csv("dbfs:/mnt/lsde/opensky/2017/*/*.csv/*.gz")
val df_20171009=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171009_L.parquet")
val df_20171008=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171008_L.parquet")
val df_20171010=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171010_L.parquet")
val df_20171011=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171011_L.parquet")
val df_20171012=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171012_L.parquet")
val df_20171013=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171013_L.parquet")
val df_20171014=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171014_L.parquet")
val df=df_20171009.unionAll(df_20171008).unionAll(df_20171010).unionAll(df_20171011).unionAll(df_20171012).unionAll(df_20171013).unionAll(df_20171014)

// COMMAND ----------

val df_repartition=df.rdd.getNumPartitions

// COMMAND ----------


val df_Icao_Callsign=df.filter(Mode.CheckFormat($"rawMessage")).select(Mode.Icao24($"rawMessage").as("Icao"),Mode.Callsign($"rawMessage").alias("Callsign")).filter(row=> !row.anyNull).dropDuplicates("Icao")


//val df_Icao_Callsign=df.dropDuplicates("rawMessage").filter(Mode.CheckFormat($"rawMessage")).select(Mode.Icao24($"rawMessage").as("Icao"),Mode.Callsign($"rawMessage").alias("Callsign")).filter(row=> !row.anyNull).dropDuplicates($"Icao")
//val df_Icao_Callsign=df.filter(Mode.CheckFormat($"rawMessage")).select(Mode.Icao24($"rawMessage").as("Icao"),Mode.Callsign($"rawMessage").alias("Callsign")).filter(row=> !row.anyNull)
//val df_mycallsign=df_Icao_Callsign.groupBy("Icao").count.groupBy("count").count
//df_mycallsign.show()

// COMMAND ----------

// display(df_Icao_Callsign)

// COMMAND ----------

df_Icao_Callsign.write.parquet("dbfs:/mnt/group16/df_icao_callsign_2017.parquet")

// COMMAND ----------

val df_Callsign=df_Icao_Callsign.dropDuplicates("Callsign").select("Callsign")

// COMMAND ----------

df_Callsign.write.parquet("dbfs:/mnt/group16/df_callsign_2017.parquet")

// COMMAND ----------



// COMMAND ----------

// DBTITLE 1,创建 callsign_2017 列表
val df_Icao_Callsign=spark.read.option("header", "true").parquet("dbfs:/mnt/group16/df_icao_callsign_2017.parquet")

// COMMAND ----------

val df_Callsign=df_Icao_Callsign.dropDuplicates("Callsign").select("Callsign")

// COMMAND ----------

df_Callsign.write.parquet("dbfs:/mnt/group16/df_callsign_2017.parquet")

// COMMAND ----------

