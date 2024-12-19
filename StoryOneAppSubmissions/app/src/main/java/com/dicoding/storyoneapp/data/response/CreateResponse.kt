package com.dicoding.storyoneapp.data.response

import com.google.gson.annotations.SerializedName

data class CreateResponse(

	@field:SerializedName("error")
	val error: Boolean? = null,

	@field:SerializedName("message")
	val message: String? = null
)
