package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.InfoModel
import com.example.myapplication.repository.MainActivityRepository
import org.json.JSONObject

class MainActivityViewModel : ViewModel() {

    var servicesLiveData: MutableLiveData<JSONObject>? = null

    fun getUser(inf: InfoModel): LiveData<JSONObject>? {
        servicesLiveData = MainActivityRepository.sendInfoApiCall(inf)
        return servicesLiveData
    }

    fun getUserResponse(): MutableLiveData<JSONObject> {
        return MainActivityRepository.serviceSetterGetter
    }

}