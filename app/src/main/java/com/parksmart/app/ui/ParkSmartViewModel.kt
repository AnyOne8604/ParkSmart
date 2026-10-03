package com.parksmart.app.ui

import androidx.lifecycle.ViewModel
import com.parksmart.app.data.DashboardData
import com.parksmart.app.data.DemoUser
import com.parksmart.app.data.ParkSmartRepository

class ParkSmartViewModel(private val repository: ParkSmartRepository) : ViewModel() {
    fun signIn(email: String, password: String): DemoUser = repository.signIn(email, password)

    val dashboard: DashboardData = repository.loadDashboard()
}
