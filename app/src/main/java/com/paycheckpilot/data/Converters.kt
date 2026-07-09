package com.paycheckpilot.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun payFrequencyToString(value: PayFrequency?): String? = value?.name

    @TypeConverter
    fun stringToPayFrequency(value: String?): PayFrequency? = value?.let(PayFrequency::valueOf)

    @TypeConverter
    fun repeatTypeToString(value: RepeatType?): String? = value?.name

    @TypeConverter
    fun stringToRepeatType(value: String?): RepeatType? = value?.let(RepeatType::valueOf)

    @TypeConverter
    fun bankConnectionStatusToString(value: BankConnectionStatus?): String? = value?.name

    @TypeConverter
    fun stringToBankConnectionStatus(value: String?): BankConnectionStatus? =
        value?.let(BankConnectionStatus::valueOf)
}
