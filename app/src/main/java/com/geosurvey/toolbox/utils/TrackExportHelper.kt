package com.geosurvey.toolbox.utils

import android.content.Context
import android.os.Environment
import com.geosurvey.toolbox.data.model.TrackPointEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class TrackExportHelper(private val context: Context) {
    
    companion object {
        private const val GPX_HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1" creator="GeoSurveyToolbox">
    <metadata>
        <name>地质勘查轨迹</name>
        <time>%s</time>
    </metadata>
    <trk>
        <name>轨迹</name>
        <trkseg>
"""
        
        private const val GPX_FOOTER = """
        </trkseg>
    </trk>
</gpx>
"""
        
        private const val KML_HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
    <Document>
        <name>地质勘查轨迹</name>
        <Style id="trackStyle">
            <LineStyle>
                <color>ff0000ff</color>
                <width>3</width>
            </LineStyle>
        </Style>
        <Placemark>
            <name>轨迹</name>
            <styleUrl>#trackStyle</styleUrl>
            <LineString>
                <altitudeMode>absolute</altitudeMode>
                <coordinates>
"""
        
        private const val KML_FOOTER = """
                </coordinates>
            </LineString>
        </Placemark>
    </Document>
</kml>
"""
    }
    
    fun exportToGPX(points: List<TrackPointEntity>, filename: String = ""): File? {
        if (points.isEmpty()) return null
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val now = sdf.format(Date())
        
        val sb = StringBuilder()
        sb.append(String.format(GPX_HEADER, now))
        
        for (point in points) {
            val time = sdf.format(Date(point.timestamp))
            sb.append("            <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
            point.altitude?.let { sb.append("                <ele>$it</ele>\n") }
            point.speed?.let { sb.append("                <speed>$it</speed>\n") }
            sb.append("                <time>$time</time>\n")
            sb.append("            </trkpt>\n")
        }
        
        sb.append(GPX_FOOTER)
        
        return saveToFile(sb.toString(), filename, "gpx")
    }
    
    fun exportToKML(points: List<TrackPointEntity>, filename: String = ""): File? {
        if (points.isEmpty()) return null
        
        val sb = StringBuilder()
        sb.append(KML_HEADER)
        
        for (point in points) {
            sb.append("                    ${point.longitude},${point.latitude},${point.altitude ?: 0.0}\n")
        }
        
        sb.append(KML_FOOTER)
        
        return saveToFile(sb.toString(), filename, "kml")
    }
    
    private fun saveToFile(content: String, filename: String, extension: String): File? {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "GeoSurveyToolbox"
            )
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = if (filename.isNotEmpty()) {
                "$filename.$extension"
            } else {
                "track_${sdf.format(Date())}.$extension"
            }
            
            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
