package com.example.breathein.network

import android.os.Parcel
import android.os.Parcelable


data class SensorData(
    val timestamp: Long,
    val frequency_0: Float,
    val frequency_1: Float,
    val bmeT: Float,
    val bmeRH: Float,
    val ensTvoc: Float,
    val ensCO2: Float,
    val axX: Float,
    val axY: Float,
    val axZ: Float
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(timestamp)
        parcel.writeFloat(frequency_0)
        parcel.writeFloat(frequency_1)
        parcel.writeFloat(bmeT)
        parcel.writeFloat(bmeRH)
        parcel.writeFloat(ensTvoc)
        parcel.writeFloat(ensCO2)
        parcel.writeFloat(axX)
        parcel.writeFloat(axY)
        parcel.writeFloat(axZ)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SensorData> {
        override fun createFromParcel(parcel: Parcel): SensorData {
            return SensorData(parcel)
        }

        override fun newArray(size: Int): Array<SensorData?> {
            return arrayOfNulls(size)
        }
    }
}
