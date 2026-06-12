package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "projects")
data class EstimationProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: Long,
    val plotArea: Double,
    val buildArea: Double,
    val floorsCount: Int,
    val buildingType: String, // STRUCTURAL, LOAD_BEARING
    val foundationType: String, // RAFT, STRIP
    val materialType: String, // YELLOW_BRICK, RED_BRICK, BLOCK, THERMOSTONE
    val roomsJson: String, // JSON serialization of List<RoomDetail>
    
    // Cached calculation results
    val totalCost: Double,
    val cementTons: Double,
    val steelTons: Double,
    val sandM3: Double,
    val gravelM3: Double,
    val unitsCount: Int,

    // Interior Finishes Custom Settings
    val flooringType: String = "PORCELAIN",
    val ceilingType: String = "GYPSUM",
    val doorsCount: Int = 6,
    val doorUnitPrice: Double = 180000.0,
    val windowsCount: Int = 5,
    val windowUnitPrice: Double = 150000.0,
    val sanitaryQuality: String = "STANDARD",
    val electricalQuality: String = "STANDARD",

    // Land reclamation optional settings
    val isReclamationEnabled: Boolean = false,
    val reclamationCost: Double = 0.0,
    val reclamationNature: String = "NATURAL",
    val reclamationExplanation: String = ""
)

@Serializable
data class RoomDetail(
    val id: String,
    val name: String,
    val width: Double,
    val length: Double,
    val height: Double
)
