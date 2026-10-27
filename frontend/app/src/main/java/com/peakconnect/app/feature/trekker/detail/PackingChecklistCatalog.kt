package com.peakconnect.app.feature.trekker.detail

import com.peakconnect.app.R
import com.peakconnect.app.data.local.entity.ChecklistItemEntity
import com.peakconnect.app.domain.model.DifficultyLevel

enum class PackingCategory(val labelRes: Int) {
    CLOTHING(R.string.checklist_category_clothing),
    GEAR(R.string.checklist_category_gear),
    SAFETY(R.string.checklist_category_safety),
    FOOD_AND_WATER(R.string.checklist_category_food_water),
    DOCUMENTS(R.string.checklist_category_documents)
}

data class CategorizedChecklistItem(
    val id: Long,
    val category: PackingCategory,
    val displayText: String,
    val isChecked: Boolean,
    val isCustom: Boolean,
    val rawEntity: ChecklistItemEntity
)

object PackingChecklistCatalog {

    fun getDefaultItems(difficulty: DifficultyLevel, activityId: String): List<ChecklistItemEntity> {
        val items = when (difficulty) {
            DifficultyLevel.EASY -> listOf(
                // Clothing
                PackingCategory.CLOTHING to "Moisture-wicking T-shirt",
                PackingCategory.CLOTHING to "Lightweight trekking pants",
                PackingCategory.CLOTHING to "Comfortable walking / hiking shoes",
                PackingCategory.CLOTHING to "Sun hat or cap",
                PackingCategory.CLOTHING to "Light rain jacket",
                // Gear
                PackingCategory.GEAR to "Daypack (15–20L)",
                PackingCategory.GEAR to "UV protection sunglasses",
                PackingCategory.GEAR to "Small quick-dry towel",
                PackingCategory.GEAR to "Trekking pole (optional)",
                // Safety
                PackingCategory.SAFETY to "Personal first-aid kit & blister bandages",
                PackingCategory.SAFETY to "Sunscreen (SPF 50+) & lip balm",
                PackingCategory.SAFETY to "Insect repellent",
                // Food and water
                PackingCategory.FOOD_AND_WATER to "Reusable water bottle (1.5L+)",
                PackingCategory.FOOD_AND_WATER to "Energy bars & trail mix snacks",
                PackingCategory.FOOD_AND_WATER to "Electrolyte sachets",
                // Documents
                PackingCategory.DOCUMENTS to "Government photo ID copy",
                PackingCategory.DOCUMENTS to "Booking confirmation & emergency card"
            )

            DifficultyLevel.MODERATE -> listOf(
                // Clothing
                PackingCategory.CLOTHING to "Moisture-wicking base layers",
                PackingCategory.CLOTHING to "Sturdy ankle-support trekking boots",
                PackingCategory.CLOTHING to "Fleece or warm mid-layer",
                PackingCategory.CLOTHING to "Waterproof rain jacket & rain cover",
                PackingCategory.CLOTHING to "Quick-dry trekking socks (2 pairs)",
                PackingCategory.CLOTHING to "Sun hat & warm fleece beanie",
                // Gear
                PackingCategory.GEAR to "Trekking backpack (30–40L)",
                PackingCategory.GEAR to "Pair of adjustable trekking poles",
                PackingCategory.GEAR to "Headlamp with extra batteries",
                PackingCategory.GEAR to "Power bank (10,000+ mAh)",
                PackingCategory.GEAR to "UV sunglasses",
                // Safety
                PackingCategory.SAFETY to "Comprehensive first-aid kit & personal meds",
                PackingCategory.SAFETY to "High-SPF sunscreen & cold cream",
                PackingCategory.SAFETY to "Safety whistle & pocket knife",
                // Food and water
                PackingCategory.FOOD_AND_WATER to "Insulated water bottle or bladder (2L)",
                PackingCategory.FOOD_AND_WATER to "High-energy snacks & dry fruits",
                PackingCategory.FOOD_AND_WATER to "Electrolyte sachets & water purification tablets",
                // Documents
                PackingCategory.DOCUMENTS to "Government photo ID",
                PackingCategory.DOCUMENTS to "Trek booking details & local permit copies",
                PackingCategory.DOCUMENTS to "Cash in small bills (remote areas)"
            )

            DifficultyLevel.CHALLENGING -> listOf(
                // Clothing
                PackingCategory.CLOTHING to "Thermal base layers (merino wool/synthetic)",
                PackingCategory.CLOTHING to "Sturdy waterproof high-ankle boots",
                PackingCategory.CLOTHING to "Down jacket / insulated puffer",
                PackingCategory.CLOTHING to "Hard-shell waterproof windbreaker & pants",
                PackingCategory.CLOTHING to "Thermal gloves & fleece inner gloves",
                PackingCategory.CLOTHING to "Heavy trekking socks (3 pairs)",
                PackingCategory.CLOTHING to "Balaclava / thermal neck gaiter",
                // Gear
                PackingCategory.GEAR to "Technical backpack (40–50L) with rain cover",
                PackingCategory.GEAR to "Heavy-duty trekking poles with snow baskets",
                PackingCategory.GEAR to "High-output headlamp (200+ lumens) + spare batteries",
                PackingCategory.GEAR to "Large power bank (20,000 mAh)",
                PackingCategory.GEAR to "Sleeping bag liner",
                // Safety
                PackingCategory.SAFETY to "Wilderness first-aid kit with altitude medication",
                PackingCategory.SAFETY to "Crepe bandages & blister relief pads",
                PackingCategory.SAFETY to "Emergency thermal foil blanket",
                PackingCategory.SAFETY to "UV Category 3/4 glacier glasses",
                // Food and water
                PackingCategory.FOOD_AND_WATER to "Hydration bladder (2L) & insulated thermos (1L)",
                PackingCategory.FOOD_AND_WATER to "High-calorie nutrition bars & nut mix",
                PackingCategory.FOOD_AND_WATER to "Electrolyte & mineral rehydration salts",
                PackingCategory.FOOD_AND_WATER to "Water purification filter or tablets",
                // Documents
                PackingCategory.DOCUMENTS to "Government ID & mountain permit documents",
                PackingCategory.DOCUMENTS to "Medical / evacuation insurance details",
                PackingCategory.DOCUMENTS to "Sufficient cash for mountain checkpoints"
            )

            DifficultyLevel.EXTREME -> listOf(
                // Clothing
                PackingCategory.CLOTHING to "Expedition thermal base layers (top & bottom)",
                PackingCategory.CLOTHING to "Mountaineering / alpine crampon-compatible boots",
                PackingCategory.CLOTHING to "Sub-zero expedition down parka (-20°C rated)",
                PackingCategory.CLOTHING to "Technical storm-proof shell jacket & bib pants",
                PackingCategory.CLOTHING to "Expedition insulated mittens & windproof liners",
                PackingCategory.CLOTHING to "Heavy merino wool socks (4 pairs)",
                PackingCategory.CLOTHING to "Thermal balaclava & fleece neck gaiter",
                // Gear
                PackingCategory.GEAR to "Technical expedition pack (55–65L) + storm cover",
                PackingCategory.GEAR to "Rigid mountaineering poles & microspikes / crampons",
                PackingCategory.GEAR to "High-lumen cold-resistant headlamp (300+ lumens)",
                PackingCategory.GEAR to "Insulated cold-proof thermos flask (1L)",
                PackingCategory.GEAR to "Insulated pouch for electronics & high-capacity power bank",
                // Safety
                PackingCategory.SAFETY to "Advanced alpine first-aid kit & pulse oximeter",
                PackingCategory.SAFETY to "High-altitude safety & AMS medications",
                PackingCategory.SAFETY to "Mountain safety whistle & rescue mirror",
                PackingCategory.SAFETY to "Emergency survival bivy bag & thermal blankets",
                PackingCategory.SAFETY to "Category 4 UV glacier goggles",
                // Food and water
                PackingCategory.FOOD_AND_WATER to "Insulated water bottles (3L total)",
                PackingCategory.FOOD_AND_WATER to "High-density endurance gels & freeze-dried rations",
                PackingCategory.FOOD_AND_WATER to "Electrolyte & mineral replenishment packs",
                PackingCategory.FOOD_AND_WATER to "Portable back-country water filter",
                // Documents
                PackingCategory.DOCUMENTS to "Government photo ID & alpine climbing permits",
                PackingCategory.DOCUMENTS to "High-altitude medical/evacuation insurance policy",
                PackingCategory.DOCUMENTS to "Laminated emergency contact sheet & emergency cash"
            )
        }

        return items.map { (cat, text) ->
            ChecklistItemEntity(
                activityId = activityId,
                difficulty = difficulty,
                text = "${cat.name}|$text",
                isChecked = false,
                isCustom = false
            )
        }
    }

    fun parseItem(entity: ChecklistItemEntity): CategorizedChecklistItem {
        val rawText = entity.text
        return if (rawText.contains("|")) {
            val catName = rawText.substringBefore("|")
            val cleanText = rawText.substringAfter("|")
            val category = try {
                PackingCategory.valueOf(catName)
            } catch (_: Exception) {
                PackingCategory.GEAR
            }
            CategorizedChecklistItem(
                id = entity.id,
                category = category,
                displayText = cleanText,
                isChecked = entity.isChecked,
                isCustom = entity.isCustom,
                rawEntity = entity
            )
        } else {
            CategorizedChecklistItem(
                id = entity.id,
                category = PackingCategory.GEAR,
                displayText = rawText,
                isChecked = entity.isChecked,
                isCustom = entity.isCustom,
                rawEntity = entity
            )
        }
    }

    fun createCustomEntity(
        activityId: String,
        difficulty: DifficultyLevel,
        category: PackingCategory,
        text: String
    ): ChecklistItemEntity {
        return ChecklistItemEntity(
            activityId = activityId,
            difficulty = difficulty,
            text = "${category.name}|${text.trim()}",
            isChecked = false,
            isCustom = true
        )
    }
}
