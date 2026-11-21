package com.writeright.ai

import java.util.Locale

/**
 * One spelling mistake. Index ranges point at the letters that differ, so the UI can
 * circle them on the board (original) and highlight them in the letter tiles (corrected).
 */
internal data class WordFix(
    val original: String,
    val corrected: String,
    val badStart: Int,
    val badEnd: Int,
    val fixStart: Int,
    val fixEnd: Int,
    val label: String,
    val tip: String
)

private const val RAW_PAIRS = """
teh=the
hte=the
adn=and
taht=that
thsi=this
wiht=with
whcih=which
wich=which
thier=their
recieve=receive
recieved=received
reciept=receipt
emale=email
becuase=because
becouse=because
wierd=weird
definately=definitely
definatly=definitely
seperate=separate
occured=occurred
ocurred=occurred
occurence=occurrence
untill=until
tommorow=tomorrow
tomorow=tomorrow
freind=friend
firend=friend
freinds=friends
beleive=believe
acheive=achieve
adress=address
begining=beginning
beutiful=beautiful
beatiful=beautiful
calender=calendar
collegue=colleague
comming=coming
completly=completely
enviroment=environment
existance=existence
experiance=experience
foriegn=foreign
goverment=government
grammer=grammar
happend=happened
immediatly=immediately
independant=independent
independance=independence
knowlege=knowledge
lenght=length
strenght=strength
heigth=height
hieght=height
neccessary=necessary
necesary=necessary
noticable=noticeable
occassion=occasion
persistant=persistent
posession=possession
prefered=preferred
publically=publicly
realy=really
recomend=recommend
reccomend=recommend
recomendation=recommendation
refered=referred
relevent=relevant
sucess=success
succesful=successful
suprise=surprise
tounge=tongue
truely=truly
wellcome=welcome
writting=writing
writen=written
diffrent=different
diffrence=difference
intresting=interesting
inteligent=intelligent
langauge=language
peice=piece
neice=niece
pasword=password
accomodate=accommodate
bussiness=business
buisness=business
catagory=category
commited=committed
dissapear=disappear
disapoint=disappoint
embarass=embarrass
excellant=excellent
excelent=excellent
finaly=finally
gaurd=guard
maintainance=maintenance
oppurtunity=opportunity
questionaire=questionnaire
rythm=rhythm
speach=speech
sincerly=sincerely
tendancy=tendency
vegtable=vegetable
yesturday=yesterday
guarentee=guarantee
scedule=schedule
shedule=schedule
sentance=sentence
responsability=responsibility
arguement=argument
apparantly=apparently
basicly=basically
curiousity=curiosity
exagerate=exaggerate
mispell=misspell
personel=personnel
priviledge=privilege
pronounciation=pronunciation
religous=religious
resturant=restaurant
thourough=thorough
unfortunatly=unfortunately
usefull=useful
yeild=yield
excersize=exercise
excercise=exercise
foward=forward
gratefull=grateful
happines=happiness
libary=library
lisence=license
pleasent=pleasant
probaly=probably
proffesor=professor
profesional=professional
twelth=twelfth
"""

private val MISSPELLINGS: Map<String, String> by lazy {
    RAW_PAIRS.lineSequence()
        .map { it.trim() }
        .filter { it.contains('=') }
        .associate { it.substringBefore('=') to it.substringAfter('=') }
}

private val TIPS: Map<String, String> = mapOf(
    "the" to "the بتتكتب t ثم h ثم e. خلي بالك من الترتيب.",
    "their" to "their فيها كلمة heir جواها: ei مش ie.",
    "which" to "which بتبدأ بـ wh زي what و when.",
    "email" to "email = e + mail. افتكرها electronic mail: e-m-a-i-l.",
    "because" to "جملة تسهّلها: Big Elephants Can Always Understand Small Elephants = because.",
    "weird" to "weird استثناء من قاعدة i قبل e. اكتبها w-e-i-r-d.",
    "definitely" to "فيها كلمة finite جواها: de-finite-ly.",
    "separate" to "افتكر a rat في النص: sep-a-rat-e.",
    "necessary" to "حرف c واحد وحرفين s: one collar, two sleeves.",
    "believe" to "فيها كلمة lie: be-lie-ve.",
    "friend" to "friend بتخلص بـ end، وصاحبك معاك لحد النهاية.",
    "tomorrow" to "حرف m واحد وحرفين r: to-mor-row.",
    "until" to "until فيها l واحد بس، عكس till.",
    "address" to "address فيها حرفين d وحرفين s.",
    "beautiful" to "ابدأ بـ beau وبعدين ti وبعدين ful.",
    "different" to "dif-fer-ent: حرفين f في الأول.",
    "restaurant" to "فيها au في النص: rest-au-rant.",
    "library" to "li-brar-y: فيها r بعد حرف b.",
    "success" to "حرفين c وحرفين s: suc-cess.",
    "grammar" to "grammar بتخلص بـ ar مش er.",
    "embarrass" to "حرفين r وحرفين s: em-bar-rass.",
    "occurred" to "حرفين c وحرفين r: oc-cur-red.",
    "business" to "busi + ness. ركز على الجزء الأول busi.",
    "length" to "length فيها g قبل th: len-g-th.",
    "height" to "height بتخلص بـ ght زي weight و light."
)

private const val CEI_TIP = "بعد حرف c ييجي e الأول وبعدين i، زي ceiling و receive."

internal fun toArabicDigits(n: Int): String =
    n.toString().map { c -> if (c in '0'..'9') '٠' + (c - '0') else c }.joinToString("")

private fun lettersAr(n: Int): String = when (n) {
    1 -> "حرف"
    2 -> "حرفين"
    in 3..10 -> "${toArabicDigits(n)} حروف"
    else -> "${toArabicDigits(n)} حرف"
}

/** Returns a fix when [raw] (one handwritten word, punctuation allowed) is a known misspelling. */
internal fun lookupFix(raw: String): WordFix? {
    val token = raw.trim { !it.isLetter() && it != '\'' }
    if (token.length < 2) return null
    val lower = token.lowercase(Locale.US)
    val right = MISSPELLINGS[lower] ?: return null
    val capital = token.first().isUpperCase()
    val original = if (capital) lower.replaceFirstChar { it.uppercaseChar() } else lower
    val corrected = if (capital) right.replaceFirstChar { it.uppercaseChar() } else right

    // Find the span of letters that differs, from both ends.
    var prefix = 0
    while (prefix < lower.length && prefix < right.length && lower[prefix] == right[prefix]) prefix++
    var suffix = 0
    while (suffix < lower.length - prefix && suffix < right.length - prefix &&
        lower[lower.length - 1 - suffix] == right[right.length - 1 - suffix]
    ) suffix++
    val badStart = prefix
    val badEnd = lower.length - suffix
    val fixStart = prefix
    val fixEnd = right.length - suffix

    val wrong = lower.substring(badStart, badEnd)
    val good = right.substring(fixStart, fixEnd)
    val swapped = wrong.length == 2 && good.length == 2 && wrong.reversed() == good

    val label = when {
        swapped -> "الغلطة في حرفين مقلوبين"
        wrong.isEmpty() -> "ناقص " + lettersAr(good.length)
        good.isEmpty() -> "فيه " + lettersAr(wrong.length) + " زيادة"
        wrong.length == good.length && wrong.length == 1 -> "الغلطة في حرف واحد"
        wrong.length == good.length -> "الغلطة في " + lettersAr(wrong.length)
        else -> "الغلطة في شكل الكلمة"
    }

    val tip = TIPS[right]
        ?: if (swapped && right.contains("cei")) CEI_TIP
        else "الكلمة الصح: $right. اكتبها تاني على اللوحة كام مرة عشان تثبت."

    return WordFix(original, corrected, badStart, badEnd, fixStart, fixEnd, label, tip)
}
