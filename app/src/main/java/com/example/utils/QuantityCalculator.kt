package com.example.utils

import com.example.data.model.RoomDetail
import kotlin.math.ceil

object QuantityCalculator {

    data class EstimationResult(
        val totalCost: Double,
        val foundationConcrete: Double,
        val columnsConcrete: Double,
        val slabsConcrete: Double,
        val totalConcrete: Double,
        
        val foundationSteel: Double,
        val columnsSteel: Double,
        val slabsSteel: Double,
        val totalSteelTons: Double,
        
        val cementTons: Double,
        val sandM3: Double,
        val gravelM3: Double,
        
        val masonryUnitsCount: Int,
        val masonryCementTons: Double,
        val masonrySandM3: Double,
        
        val flooringAreaM2: Double,
        val wallPlasterAreaM2: Double,
        val paintingAreaM2: Double,
        
        // Costs
        val concreteCost: Double,
        val steelCost: Double,
        val cementCost: Double,
        val aggregatCost: Double,
        val masonryUnitsCost: Double,
        val finishingCost: Double,
        val laborCost: Double,
        val reclamationCost: Double,

        // Sub finishes costs
        val tileCost: Double,
        val ceilingCost: Double,
        val doorsCost: Double,
        val windowsCost: Double,
        val sanitaryCost: Double,
        val electricalCost: Double,
        val plasterCost: Double,
        val paintingCost: Double,

        // Custom extra finishes
        val gessoCost: Double = 0.0,
        val pavementCost: Double = 0.0,
        val exteriorStuccoCost: Double = 0.0,
        val externalStoneCost: Double = 0.0,
        val pavementLengthM: Double = 0.0,
        val exteriorStuccoAreaM2: Double = 0.0,
        val externalStoneAreaM2: Double = 0.0,
        val finishingLaborCost: Double = 0.0
    )

    fun calculate(
        plotArea: Double,
        buildArea: Double,
        floorsCount: Int,
        buildingType: String, // "STRUCTURAL" / "LOAD_BEARING"
        foundationType: String, // "RAFT" / "STRIP"
        materialType: String, // "YELLOW_BRICK" / "RED_BRICK" / "BLOCK" / "THERMOSTONE"
        rooms: List<RoomDetail>,
        
        // Materials prices
        cementPrice: Double, // price per ton
        steelPrice: Double,  // price per ton
        sandPrice: Double,   // price per cubic meter
        gravelPrice: Double, // price per cubic meter
        brickPrice: Double,  // price per 1000 bricks/blocks
        laborPricePerM2: Double, // general labor price per square meter of building area

        // Primary Finishes Prices (Customizable in Settings)
        plasterPricePerM2: Double = 8000.0,
        gessoPricePerM2: Double = 4000.0,
        ceramicPricePerM2: Double = 10000.0,
        porcelainPricePerM2: Double = 18000.0,
        marblePricePerM2: Double = 35000.0,
        pipingPricePerM2: Double = 5000.0,
        wiringPricePerM2: Double = 6000.0,

        // Optional Finishes Prices (Customizable in Settings)
        ceilingPlasterPricePerM2: Double = 5000.0,
        ceilingPvcPricePerM2: Double = 12000.0,
        ceilingGypsumPricePerM2: Double = 15000.0,
        paintingPricePerM2: Double = 6000.0,
        pavementPricePerMeter: Double = 15000.0,
        exteriorStuccoPricePerM2: Double = 7000.0,
        externalStonePricePerM2: Double = 45000.0,

        sanitaryBasicPrice: Double = 1200000.0,
        sanitaryStandardPrice: Double = 2500000.0,
        sanitaryLuxuryPrice: Double = 4500000.0,
        
        electricalBasicPrice: Double = 1000000.0,
        electricalStandardPrice: Double = 2200000.0,
        electricalSmartPrice: Double = 4000000.0,

        // Selection configuration
        flooringType: String = "PORCELAIN",
        ceilingType: String = "GYPSUM",
        
        isCeilingsEnabled: Boolean = true,
        isDoorsEnabled: Boolean = true,
        doorsCount: Int = 6,
        doorUnitPrice: Double = 180000.0,
        
        isWindowsEnabled: Boolean = true,
        windowsCount: Int = 5,
        windowUnitPrice: Double = 150000.0,
        
        // Custom Toggles for Optional Finishes
        isPaintingEnabled: Boolean = true,
        isPavementEnabled: Boolean = false,
        isExteriorStuccoEnabled: Boolean = false,
        isExternalStoneEnabled: Boolean = false,
        
        sanitaryQuality: String = "STANDARD",
        electricalQuality: String = "STANDARD",
        reclamationCost: Double = 0.0,
        isManualLaborEnabled: Boolean = false,
        manualLaborCost: Double = 0.0,
        isManualFinishingLaborEnabled: Boolean = false,
        finishingLaborPricePerM2: Double = 15000.0,
        manualFinishingLaborCost: Double = 0.0
    ): EstimationResult {
        // Room parameters must be defined first to use for structural wall calculations
        val totalRoomFloorArea = if (rooms.isNotEmpty()) {
            rooms.sumOf { it.width * it.length }
        } else {
            buildArea * floorsCount * 0.75 // assume 75% of building is rooms
        }

        val totalRoomPerimeter = if (rooms.isNotEmpty()) {
            rooms.sumOf { 2 * (it.width + it.length) }
        } else {
            // formula derived based on estimated wall lengths
            Math.sqrt(buildArea) * 4 * floorsCount * 1.5 
        }

        val totalWallHeight = if (rooms.isNotEmpty()) {
            rooms.sumOf { it.height } / rooms.size.toDouble()
        } else {
            3.0
        }

        val wallPlasterAreaM2 = totalRoomPerimeter * totalWallHeight
        val paintingAreaM2 = wallPlasterAreaM2 + totalRoomFloorArea // walls + ceilings
        val flooringAreaM2 = totalRoomFloorArea

        // 1. Concrete Volumes (in cubic meters - m3)
        // Foundations
        val foundationThickness = if (foundationType == "RAFT") 0.45 else 0.20
        val foundationConcrete = buildArea * foundationThickness

        // Columns or Load-Bearing Tie Beams
        val columnsPerFloor = ceil(buildArea / 15.0).toInt()
        val totalColumns = if (buildingType == "STRUCTURAL") {
            columnsPerFloor * floorsCount
        } else {
            0
        }

        val wallWidth = if (materialType == "YELLOW_BRICK" || materialType == "RED_BRICK") 0.24 else 0.20
        val columnsConcrete = if (buildingType == "STRUCTURAL") {
            totalColumns * (0.3 * 0.3 * 3.1) // typical column size 30x30cm, height 3.1m
        } else {
            // LOAD_BEARING: No columns. Continuous concrete tie beam (رباط خرساني مستمر) running above walls
            // thickness of 0.3m, width = wallWidth, at height 2.2m (which is above standard 2.0m doors by 20cm/0.2m)
            totalRoomPerimeter * wallWidth * 0.30
        }

        // Slabs
        val slabConcretePerFloor = buildArea * 0.20 // 20cm thickness
        val slabsConcrete = slabConcretePerFloor * floorsCount

        val totalConcrete = foundationConcrete + columnsConcrete + slabsConcrete

        // 2. Steel calculations (in tons)
        val foundationSteelDensity = if (foundationType == "RAFT") 90.0 else 60.0
        val foundationSteel = (foundationConcrete * foundationSteelDensity) / 1000.0

        val columnsSteelDensity = 125.0
        val columnsSteel = (columnsConcrete * columnsSteelDensity) / 1000.0

        val slabsSteelDensity = 85.0
        val slabsSteel = (slabsConcrete * slabsSteelDensity) / 1000.0

        val totalSteelTons = foundationSteel + columnsSteel + slabsSteel

        // 3. Concrete Materials (Cement, Sand, Gravel)
        // Standard mix ratio: 1:2:4 means 1 m3 concrete requires ~350kg cement, 0.43m3 sand, 0.86m3 gravel
        val concreteCementTons = (totalConcrete * 0.35) 
        val concreteSandM3 = totalConcrete * 0.43
        val concreteGravelM3 = totalConcrete * 0.86

        // 5. Masonry Units (bricks or blocks) depending on material type
        // Let's compute wall area for masonry. In LOAD_BEARING, we subtract the tie-beam height (0.3m) from bricklaying height!
        val adjustedWallHeightForMasonry = if (buildingType == "LOAD_BEARING") {
            (totalWallHeight - 0.30).coerceAtLeast(1.5)
        } else {
            totalWallHeight
        }
        val masonryWallArea = totalRoomPerimeter * adjustedWallHeightForMasonry * 0.85 // minus doors/windows

        var unitsCount = 0
        var masonryCementTons = 0.0
        var masonrySandM3 = 0.0

        when (materialType) {
            "YELLOW_BRICK", "RED_BRICK" -> {
                // Iraqi standard brick: 24 x 11.5 x 7.5 cm
                // Double brick wall or standard thickness 24cm takes about 110 bricks/m2 of wall
                // Each m2 of wall takes 110 bricks, 24kg cement, and 0.06m3 sand
                unitsCount = (masonryWallArea * 110).toInt()
                masonryCementTons = (unitsCount / 1000.0) * 0.22
                masonrySandM3 = (unitsCount / 1000.0) * 0.55
            }
            "BLOCK" -> {
                // Concrete blocks: 40 x 20 x 20 cm
                // standard wall takes 12.5 blocks/m2 (0.08 m2 each)
                unitsCount = (masonryWallArea * 12.5).toInt()
                masonryCementTons = masonryWallArea * 0.015
                masonrySandM3 = masonryWallArea * 0.03
            }
            "THERMOSTONE" -> {
                // Lightweight concrete block: 60 x 20 x 20 cm
                // wall takes about 8.33 blocks per m2 (0.12 m2 each)
                unitsCount = (masonryWallArea * 8.33).toInt()
                masonryCementTons = masonryWallArea * 0.01 // special thin mortar or adhesive
                masonrySandM3 = masonryWallArea * 0.015
            }
        }

        // Aggregate total materials
        val cementTons = concreteCementTons + masonryCementTons
        val sandM3 = concreteSandM3 + masonrySandM3
        val gravelM3 = concreteGravelM3

        // 6. Cost calculations
        val steelCost = totalSteelTons * steelPrice
        val cementCost = cementTons * cementPrice
        val aggregateCost = (sandM3 * sandPrice) + (gravelM3 * gravelPrice)
        
        val masonryUnitsPriceRef = when (materialType) {
            "YELLOW_BRICK" -> brickPrice // per 1000
            "RED_BRICK" -> brickPrice
            "BLOCK" -> brickPrice
            "THERMOSTONE" -> brickPrice
            else -> brickPrice
        }
        val masonryUnitsCost = (unitsCount / 1000.0) * masonryUnitsPriceRef

        // Concrete mixing, shuttering labor or dynamic
        val concreteCost = totalConcrete * 55000.0 // avg iqd concrete materials cost / labor

        // --- PRIMARY/MANDATORY FINISHES COSTS ---
        // 1. اللبخ (Plastering)
        val plasterCost = wallPlasterAreaM2 * plasterPricePerM2
        
        // 2. البياض بالبورق (Rendering)
        val gessoCost = wallPlasterAreaM2 * gessoPricePerM2

        // 3. السيراميك والتطبيق (Tiling)
        val flooringPricePerM2 = when(flooringType) {
            "CERAMIC" -> ceramicPricePerM2
            "MARBLE" -> marblePricePerM2
            else -> porcelainPricePerM2 // PORCELAIN or default
        }
        val tileCost = flooringAreaM2 * flooringPricePerM2

        // 4. التأسيس المائي والسباكة (Plumbing)
        val sanitaryCost = when(sanitaryQuality) {
            "BASIC" -> sanitaryBasicPrice
            "LUXURY" -> sanitaryLuxuryPrice
            else -> sanitaryStandardPrice // STANDARD
        }

        // 5. التأسيس الكهربائي والإنارة (Electrical)
        val electricalCost = when(electricalQuality) {
            "BASIC" -> electricalBasicPrice
            "SMART" -> electricalSmartPrice
            else -> electricalStandardPrice // STANDARD
        }

        // --- OPTIONAL FINISHES COSTS (Checkbox/Switch enabled) ---
        // 6. الأصباغ والدهانات الداخلية
        val paintingCost = if (isPaintingEnabled) paintingAreaM2 * paintingPricePerM2 else 0.0

        // 7. تصميم الأسقف الثانوية والجبسم بورد
        val ceilingCost = if (isCeilingsEnabled) {
            val ceilingPricePerM2 = when(ceilingType) {
                "PLASTER" -> ceilingPlasterPricePerM2
                "PVC" -> ceilingPvcPricePerM2
                else -> ceilingGypsumPricePerM2 // GYPSUM or default
            }
            flooringAreaM2 * ceilingPricePerM2
        } else {
            0.0
        }

        // 8. الأبواب
        val doorsCost = if (isDoorsEnabled) doorsCount * doorUnitPrice else 0.0

        // 9. الشبابيك
        val windowsCost = if (isWindowsEnabled) windowsCount * windowUnitPrice else 0.0

        // 10. صب دكة الحماية الخارجية المحيطة
        val perimeter = Math.sqrt(buildArea) * 4
        val pavementCost = if (isPavementEnabled) perimeter * pavementPricePerMeter else 0.0

        // 11. اللبخ الخارجي الملون أو النثر
        val extArea = Math.sqrt(buildArea) * 4 * totalWallHeight * floorsCount
        val exteriorStuccoCost = if (isExteriorStuccoEnabled) extArea * exteriorStuccoPricePerM2 else 0.0

        // 12. واجهات الحجر الطبيعي الموصلي والعزل
        val frontArea = Math.sqrt(buildArea) * totalWallHeight * floorsCount
        val externalStoneCost = if (isExternalStoneEnabled) frontArea * externalStonePricePerM2 else 0.0

        // Aggregate total detailed finishing cost
        val finishingCost = plasterCost + gessoCost + tileCost + sanitaryCost + electricalCost +
                            ceilingCost + doorsCost + windowsCost + paintingCost + 
                            pavementCost + exteriorStuccoCost + externalStoneCost

        // General construction workers contract cost (Labor)
        val laborCost = if (isManualLaborEnabled) manualLaborCost else (buildArea * floorsCount * laborPricePerM2)

        // Finishing workers contract cost (Labor)
        val finishingLaborCost = if (isManualFinishingLaborEnabled) manualFinishingLaborCost else (buildArea * floorsCount * finishingLaborPricePerM2)

        val totalCost = steelCost + cementCost + aggregateCost + masonryUnitsCost + finishingCost + laborCost + reclamationCost + finishingLaborCost

        return EstimationResult(
            totalCost = totalCost,
            foundationConcrete = foundationConcrete,
            columnsConcrete = columnsConcrete,
            slabsConcrete = slabsConcrete,
            totalConcrete = totalConcrete,
            
            foundationSteel = foundationSteel,
            columnsSteel = columnsSteel,
            slabsSteel = slabsSteel,
            totalSteelTons = totalSteelTons,
            
            cementTons = cementTons,
            sandM3 = sandM3,
            gravelM3 = gravelM3,
            
            masonryUnitsCount = unitsCount,
            masonryCementTons = masonryCementTons,
            masonrySandM3 = masonrySandM3,
            
            flooringAreaM2 = flooringAreaM2,
            wallPlasterAreaM2 = wallPlasterAreaM2,
            paintingAreaM2 = paintingAreaM2,
            
            concreteCost = concreteCost,
            steelCost = steelCost,
            cementCost = cementCost,
            aggregatCost = aggregateCost,
            masonryUnitsCost = masonryUnitsCost,
            finishingCost = finishingCost,
            laborCost = laborCost,

            tileCost = tileCost,
            ceilingCost = ceilingCost,
            doorsCost = doorsCost,
            windowsCost = windowsCost,
            sanitaryCost = sanitaryCost,
            electricalCost = electricalCost,
            plasterCost = plasterCost,
            paintingCost = paintingCost,
            reclamationCost = reclamationCost,

            gessoCost = gessoCost,
            pavementCost = pavementCost,
            exteriorStuccoCost = exteriorStuccoCost,
            externalStoneCost = externalStoneCost,
            pavementLengthM = if (isPavementEnabled) perimeter else 0.0,
            exteriorStuccoAreaM2 = if (isExteriorStuccoEnabled) extArea else 0.0,
            externalStoneAreaM2 = if (isExternalStoneEnabled) frontArea else 0.0,
            finishingLaborCost = finishingLaborCost
        )
    }
}
