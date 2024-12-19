package com.dicoding.storyoneapp.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dicoding.storyoneapp.data.api.ApiService
import com.dicoding.storyoneapp.data.response.ListStoryItem

class StoriesPagingSource(
    private val apiService: ApiService,
    private val token: String
) : PagingSource<Int, ListStoryItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListStoryItem> {
        val page = params.key ?: 1 // Mulai dari halaman 1
        return try {
            val response = apiService.getStories("Bearer $token", page, params.loadSize)
            val stories = response.listStory?.filterNotNull() ?: emptyList()

            LoadResult.Page(
                data = stories,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (stories.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ListStoryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
