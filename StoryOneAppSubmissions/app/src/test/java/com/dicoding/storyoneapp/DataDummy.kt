package com.dicoding.storyoneapp

import com.dicoding.storyoneapp.data.response.ListStoryItem

object DataDummy {

    fun generateDummyStoryResponse(): List<ListStoryItem> {
        val items: MutableList<ListStoryItem> = arrayListOf()
        for (i in 0..100) {
            val story = ListStoryItem(
                id = i.toString(), // String
                name = "name $i", // String
                description = "description $i", // String
                createdAt = "createAt $i", // String
                lat = i.toDouble(), // Double
                lon = i.toDouble() // Double
            )
            items.add(story)
        }
        return items
    }
}
