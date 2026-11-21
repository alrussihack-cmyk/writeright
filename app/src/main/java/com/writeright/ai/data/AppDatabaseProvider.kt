package com.writeright.ai.data

import android.content.Context
import com.writeright.ai.data.local.WriteRightDatabase
import com.writeright.ai.data.repository.CourseRepository

object AppDatabaseProvider {
    fun repository(context: Context): CourseRepository =
        CourseRepository(WriteRightDatabase.getInstance(context).writeRightDao())
}
