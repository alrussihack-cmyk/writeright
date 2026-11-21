package com.writeright.ai.data.seed

import com.writeright.ai.data.local.WriteRightDao
import com.writeright.ai.data.local.entity.*

object B1CurriculumSeeder {
    suspend fun seedIfEmpty(dao: WriteRightDao) {
        if (dao.lessonCount() > 0) return

        dao.insertCourse(
            CourseEntity(
                id = "english-for-work-b1",
                title = "English for Work",
                description = "Practical English writing and vocabulary for everyday workplace communication."
            )
        )

        dao.insertLessons(
            listOf(
                LessonEntity(
                    id = "b1-workplace-responsibilities",
                    courseId = "english-for-work-b1",
                    orderIndex = 1,
                    title = "Workplace responsibilities",
                    objective = "Describe your duties and responsibilities clearly.",
                    explanationArabic = "في الدرس ده هتتعلم تتكلم عن مسؤولياتك في الشغل وتكتب جمل واضحة عن مهامك.",
                    grammarTip = "Use responsible for + noun or verb ending in -ing.",
                    estimatedMinutes = 12
                ),
                LessonEntity(
                    id = "b1-shift-handover",
                    courseId = "english-for-work-b1",
                    orderIndex = 2,
                    title = "Shift handover",
                    objective = "Write a clear handover note for the next shift.",
                    explanationArabic = "هتتعلم تسلّم الشيفت وتكتب الملاحظات المهمة للفريق اللي بعدك.",
                    grammarTip = "Use the past simple for completed events and present simple for current status.",
                    estimatedMinutes = 15
                ),
                LessonEntity(
                    id = "b1-reporting-maintenance",
                    courseId = "english-for-work-b1",
                    orderIndex = 3,
                    title = "Reporting a maintenance issue",
                    objective = "Describe a problem, its location and the action required.",
                    explanationArabic = "هتتعلم توصف عطل أو مشكلة صيانة وتحدد مكانها والإجراء المطلوب.",
                    grammarTip = "Use there is/there are to report a problem and should/needs to for recommendations.",
                    estimatedMinutes = 15
                )
            )
        )

        dao.insertVocabulary(
            listOf(
                VocabularyEntity("v-responsible", "b1-workplace-responsibilities", "responsible", "مسؤول", "ri-SPON-suh-buhl", "I am responsible for supervising the team.", "أنا مسؤول عن الإشراف على الفريق."),
                VocabularyEntity("v-supervise", "b1-workplace-responsibilities", "supervise", "يشرف على", "SOO-per-vize", "I supervise the lifeguard team.", "أنا أشرف على فريق المنقذين."),
                VocabularyEntity("v-assigned", "b1-workplace-responsibilities", "assigned", "مكلّف / تم تكليفه", "uh-SIGNED", "Each employee is assigned a daily task.", "يتم تكليف كل موظف بمهمة يومية."),
                VocabularyEntity("v-ensure", "b1-workplace-responsibilities", "ensure", "يتأكد من / يضمن", "en-SHOOR", "We ensure that safety rules are followed.", "نتأكد من اتباع قواعد السلامة."),
                VocabularyEntity("v-coordinate", "b1-workplace-responsibilities", "coordinate", "ينسّق", "koh-OR-dih-nayt", "I coordinate daily activities with the team.", "أنسق الأنشطة اليومية مع الفريق."),
                VocabularyEntity("v-handover", "b1-shift-handover", "handover", "تسليم الشيفت", "HAND-oh-ver", "Please read the handover before starting your shift.", "من فضلك اقرأ تسليم الشيفت قبل بدء عملك."),
                VocabularyEntity("v-equipment", "b1-shift-handover", "equipment", "معدات", "ih-KWIP-ment", "The pool equipment is working properly.", "معدات حمام السباحة تعمل بشكل سليم."),
                VocabularyEntity("v-incident", "b1-shift-handover", "incident", "واقعة / حادث", "IN-sih-dent", "There was a minor incident near the main pool.", "حدثت واقعة بسيطة بالقرب من حمام السباحة الرئيسي."),
                VocabularyEntity("v-maintenance", "b1-reporting-maintenance", "maintenance", "صيانة", "MAYN-tuh-nuhns", "The pool is closed for maintenance.", "حمام السباحة مغلق للصيانة."),
                VocabularyEntity("v-repair", "b1-reporting-maintenance", "repair", "إصلاح", "ri-PAIR", "The technician will repair the pump.", "سيقوم الفني بإصلاح المضخة."),
                VocabularyEntity("v-leak", "b1-reporting-maintenance", "leak", "تسرّب", "LEEK", "There is a water leak near the shower area.", "يوجد تسرب مياه بالقرب من منطقة الدش."),
                VocabularyEntity("v-inspection", "b1-reporting-maintenance", "inspection", "فحص / معاينة", "in-SPEK-shun", "The daily inspection is completed.", "تم الانتهاء من الفحص اليومي.")
            )
        )

        dao.insertExercises(
            listOf(
                ExerciseEntity("ex-responsible-1", "b1-workplace-responsibilities", 1, "اكتب الكلمة الإنجليزية التي تعني «مسؤول».", "مسؤول", "responsible"),
                ExerciseEntity("ex-responsible-2", "b1-workplace-responsibilities", 2, "أكمل الجملة بالكلمة المناسبة.", "I am responsible for _____ the team.", "supervising", "WRITE_SENTENCE"),
                ExerciseEntity("ex-handover-1", "b1-shift-handover", 1, "اكتب المصطلح الذي يعني «تسليم الشيفت».", "تسليم الشيفت", "handover"),
                ExerciseEntity("ex-handover-2", "b1-shift-handover", 2, "اكتب جملة قصيرة عن حالة المعدات.", "The pool equipment is working properly.", "The pool equipment is working properly.", "WRITE_SENTENCE"),
                ExerciseEntity("ex-maintenance-1", "b1-reporting-maintenance", 1, "اكتب الكلمة التي تعني «صيانة».", "صيانة", "maintenance"),
                ExerciseEntity("ex-maintenance-2", "b1-reporting-maintenance", 2, "اكتب جملة للإبلاغ عن تسرب مياه.", "There is a water leak near the shower area.", "There is a water leak near the shower area.", "WRITE_SENTENCE")
            )
        )
    }
}
