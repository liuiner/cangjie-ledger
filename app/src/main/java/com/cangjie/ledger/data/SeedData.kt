package com.cangjie.ledger.data

object SeedData {
    val categories = listOf(
        SeedCategory("餐饮", EntryType.EXPENSE, "🍜", 0xFFE8795A, 10),
        SeedCategory("交通", EntryType.EXPENSE, "🚇", 0xFF4D83C6, 20),
        SeedCategory("购物", EntryType.EXPENSE, "🛍️", 0xFFC5689A, 30),
        SeedCategory("居住", EntryType.EXPENSE, "🏠", 0xFF8C7358, 40),
        SeedCategory("娱乐", EntryType.EXPENSE, "🎬", 0xFF7B72B7, 50),
        SeedCategory("医疗", EntryType.EXPENSE, "💊", 0xFFD35F62, 60),
        SeedCategory("学习", EntryType.EXPENSE, "📚", 0xFF3D8C8A, 70),
        SeedCategory("旅行", EntryType.EXPENSE, "✈️", 0xFF4A9E72, 80),
        SeedCategory("人情", EntryType.EXPENSE, "🎁", 0xFFCE7B36, 90),
        SeedCategory("其他", EntryType.EXPENSE, "🧾", 0xFF7A8581, 100),
        SeedCategory("工资", EntryType.INCOME, "💼", 0xFF2E8B68, 10),
        SeedCategory("奖金", EntryType.INCOME, "🏅", 0xFFD39B2E, 20),
        SeedCategory("理财", EntryType.INCOME, "📈", 0xFF4D7FC4, 30),
        SeedCategory("其他收入", EntryType.INCOME, "💰", 0xFF669174, 40),
    )
}

data class SeedCategory(
    val name: String,
    val type: String,
    val emoji: String,
    val color: Long,
    val sortOrder: Int,
)

