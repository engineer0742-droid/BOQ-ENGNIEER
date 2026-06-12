package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.model.EstimationProject
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<EstimationProject>> = projectDao.getAllProjects()

    suspend fun insertProject(project: EstimationProject): Long {
        return projectDao.insertProject(project)
    }

    suspend fun deleteProjectById(id: Int) {
        projectDao.deleteProjectById(id)
    }
}
