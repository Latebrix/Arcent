package tech.arcent.achievements.data.repo

/*
 simple mapping ext (kinda plain but keeps ui layer cleaner + avoids leaking domain fields)
 */

fun tech.arcent.achievements.domain.AchievementDomain.toUi(): tech.arcent.home.Achievement =
    tech.arcent.home.Achievement(
        id = id,
        title = title,
        achievedAt = achievedAt,
        tags = tags,
        photoUrl = photoUrl,
        details = details,
        categories = categories,
    )
