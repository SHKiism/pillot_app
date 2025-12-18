package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.InfoModel
import com.example.myapplication.repository.MainActivityRepository
import com.example.myapplication.utils.SingleLiveEvent
import org.json.JSONObject

class MainActivityViewModel : ViewModel() {

    var servicesLiveData: SingleLiveEvent<JSONObject>? = null

    fun getUser(inf: InfoModel): LiveData<JSONObject>? {
        servicesLiveData = MainActivityRepository.sendInfoApiCall(inf)
        return servicesLiveData
    }

    fun getUserResponse(): SingleLiveEvent<JSONObject> {
        return MainActivityRepository.serviceSetterGetter
    }

}