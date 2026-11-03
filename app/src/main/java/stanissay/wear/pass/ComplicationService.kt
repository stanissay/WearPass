package stanissay.wear.pass

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class ComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return SmallImageComplicationData.Builder(
            smallImage = SmallImage.Builder(
                image = Icon.createWithResource(this, R.drawable.ic_launcher),
                type = SmallImageType.PHOTO
            ).build(),
            contentDescription = PlainComplicationText.Builder("Open WearPass").build()
        )
            .setTapAction(pendingIntent)
            .build()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return SmallImageComplicationData.Builder(
            smallImage = SmallImage.Builder(
                image = Icon.createWithResource(this, R.drawable.ic_launcher),
                type = SmallImageType.PHOTO
            ).build(),
            contentDescription = PlainComplicationText.Builder("Preview").build()
        ).build()
    }
}