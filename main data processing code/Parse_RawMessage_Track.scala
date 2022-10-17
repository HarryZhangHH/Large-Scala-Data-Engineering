// Databricks notebook source
// DBTITLE 1,This file is used to parse raw dataset and detect flights
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
//       val icaostring = icao24.toString().getBytes
//       val icao = ""
//        for(i <- 0 to icaostring.length-1)
//         icao.concat(icaostring(i))
      tools.toHexString(icao24)
    } catch {
      case _: Throwable => null
    }
  }
  val Icao24 = udf[String, String](getIcao _)
}



// COMMAND ----------

// val df = spark.read.option("header",true)
//                 .csv("dbfs:/mnt/lsde/opensky/2017/20171009/*.csv/*.gz")
// val df=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171010_L.parquet")
val df_20171009=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171009_L.parquet")
val df_20171008=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171008_L.parquet")
val df_20171010=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171010_L.parquet")
val df_20171011=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171011_L.parquet")
val df_20171012=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171012_L.parquet")
val df_20171013=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171013_L.parquet")
val df_20171014=spark.read.format("delta").load("dbfs:/mnt/group16/2017/20171014_L.parquet")
val df=df_20171009.unionAll(df_20171008).unionAll(df_20171010).unionAll(df_20171011).unionAll(df_20171012).unionAll(df_20171013).unionAll(df_20171014)

// COMMAND ----------

println(df.rdd.getNumPartitions)

// COMMAND ----------

// val df_repartition=df.repartition(5000)

// COMMAND ----------

// val df_DownlinkFormat=df_repartition.dropDuplicates("rawMessage").filter(Mode.CheckFormat($"rawMessage")).select($"rawMessage",$"timeAtServer",Mode.DownlinkFormat($"rawMessage").as("DownlinkFormat")).filter("DownlinkFormat==17 or DownlinkFormat==18")
val df_DownlinkFormat=df.filter(Mode.CheckFormat($"rawMessage")).select($"rawMessage",$"timeAtServer",Mode.DownlinkFormat($"rawMessage").as("DownlinkFormat")).filter("DownlinkFormat==17 or DownlinkFormat==18")

// COMMAND ----------

val df_Icao24=df_DownlinkFormat.select($"rawMessage",$"timeAtServer",Mode.Icao24($"rawMessage").as("Icao24")).orderBy("Icao24","timeAtServer")//.repartition($"Icao24") //orderBy 用于解析位置信息

// COMMAND ----------

val df_Pos=df_Icao24.select($"rawMessage",$"timeAtServer",$"Icao24",Mode.Pos($"rawMessage",$"timeAtServer").as("Position")).filter(row=> !row.anyNull)

// COMMAND ----------

val df_Pos_seperate=df_Pos.select($"rawMessage",$"timeAtServer".cast("double"),$"Icao24",$"Position._1".as("lon"),$"Position._2".as("lat"),$"Position._3".as("alt"))

// COMMAND ----------

import org.apache.spark.sql.functions._
val df_List=df_Pos_seperate.groupBy($"Icao24").agg(collect_list("alt") as "altitude_list",collect_list("timeAtServer") as "timeAtServer_list",collect_list("lon") as "longitude_list",collect_list("lat") as "latitude_list")

// COMMAND ----------



// COMMAND ----------

import scala.collection.mutable.ListBuffer
object Tracker extends Serializable{
  def rad(number:Double): Double ={
    number*Math.PI/180
  }
  def distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = { //返回的是公里为单位的距离数
    val EARTH_RADIUS = 6378.137
    val radLat1 = rad(lat1)
    val radLat2 = rad(lat2)
    val a = rad(lat1) - rad(lat2)
    val b = rad(lng1) - rad(lng2)
    val distance = EARTH_RADIUS * 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin((b) / 2), 2)))
    //    printLog.debug("lat1: " + lat1 + " lng1: " + lng1 + " lat2: " + lat2 + " lng2: " + lng2)
//     println("distance:" + distance)
    distance
  }
  
  def getPointCluster(timeAtServer:Array[Double],alt:Array[Double],lng:Array[Double],lat:Array[Double])={
    val length=timeAtServer.length
    var cluster_number=0
    var res=ListBuffer(cluster_number)
    for(i<-5 until length-5){
      if(((timeAtServer(i)-timeAtServer(i-1)>1200000 || alt(i)-alt(i-1)>1500|| distance(lat(i),lng(i),lat(i-1),lng(i-1))>40|| alt(i-3)>alt(i) && alt(i+3)>alt(i)))&&alt(i)<3000){
        cluster_number+=1
        res.append(cluster_number)
      }else{
        res.append(cluster_number)
      }
    }
    res
  }
    def checkTrack(alt:Array[Double]):Boolean={
    var res = 0
    val length=alt.length
    if(length<100) return false
    if(alt(0)<3000){
      res+=1
    }

    if(alt(length-1)<3000){
      res+=1
    }
    if(alt(1)<alt(3) && alt(3)<alt(5)){
      res+=1
    }
    if(alt(length-5)>alt(length-3) && alt(length-3)>alt(length-1)){
      res+=1
    }
//     if(alt(length/2)>10000){
//       res+=1
//     }
    if(res==4) return true
    else return false
  }
  def getFirstElement(time:Array[Double])={
    time(0)
  }
  def getLastElement(time:Array[Double])={
    time(time.length-1)
  }
  def getMaxElement(altitude:Array[Double])={
    val length=altitude.length
    var max=0.0
    for(i<-0 until length){
      if(altitude(i)>max) max= altitude(i)
    }
    max
  }
  
  val MaxElement=udf((altitude:Array[Double])=>getMaxElement(altitude))
  val FirstElement=udf((time:Array[Double])=>getFirstElement(time))
  val LastElement=udf((time:Array[Double])=>getLastElement(time))
  val Cluster_point = udf((timeAtServer:Array[Double],alt:Array[Double],lng:Array[Double],lat:Array[Double]) => getPointCluster(timeAtServer,alt,lng,lat))
  val CheckTrack=udf((alt:Array[Double])=>checkTrack(alt))
}

// COMMAND ----------

val df_classification=df_List.select($"Icao24",$"altitude_list",$"timeAtServer_list",$"longitude_list",$"latitude_list",Tracker.Cluster_point($"timeAtServer_list",$"altitude_list",$"longitude_list",$"latitude_list") as "classification")

// COMMAND ----------

import org.apache.spark.sql.functions._
import scala.collection.mutable.ListBuffer

val df_test=df_classification.select($"Icao24",$"altitude_list",$"timeAtServer_list",$"longitude_list",$"latitude_list",$"classification",
                                     explode(
                                       arrays_zip($"longitude_list",$"latitude_list",$"timeAtServer_list",$"altitude_list",$"classification")
                                       .cast("array<struct<longitude:Double,latitude:Double,timeAtServer:Double,altitude:Double,classification:Int>>")
                                       ).as("Position")
                                     ).select($"Icao24",$"Position.longitude" as "lon",$"Position.latitude" as "lat",$"Position.altitude" as "alt",$"Position.timeAtServer" as "timeAtsServer",$"Position.classification" as "cls")

// COMMAND ----------

val df_insert=df_test.selectExpr("Icao24","round(lon,4) as lon","round(lat,4) as lat","round(alt,4) as alt","timeAtsServer","cls").dropDuplicates("Icao24","cls","lat","lon","alt")

// COMMAND ----------

val df_cls_filter=df_insert
.groupBy("Icao24","cls")
.agg(collect_list("alt") as "altitude_list",collect_list("timeAtsServer") as "timeAtServer_list",collect_list("lon") as "longitude_list",collect_list("lat") as "latitude_list")
.withColumn("quality",Tracker.CheckTrack($"altitude_list"))
.filter("quality is true")

// COMMAND ----------

// println(df_cls_filter.count())

// COMMAND ----------

df_cls_filter.write.parquet("dbfs:/mnt/group16/df_cls_filter_2017_2.parquet")

// COMMAND ----------

