package ru.btpit.nmedia.repository
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken;
import  ru.btpit.nmedia.dto.Post

class PostRepositoryFileImpl (
    private val context: Context,
    ) : PostRepository{
    private val gson = Gson()
    private val type = TypeToken.getParameterized(List::class.java, Post::class.java).type
    private val filename = "posts.json"
    private var nextId = 1
    private var posts = emptyList<Post>()
    private val data = MutableLiveData(posts)
    init{
        val file = context.filesDir.resolve(filename)
        if(file.exists()){
            context.openFileInput(filename).bufferedReader().use{
                posts = gson.fromJson(it,type)
                nextId = (posts.maxOfOrNull { it.id } ?: 0) + 1

            }
        } else {
            sync()
                }
        data.value = posts
    }
    private fun sync() {
        context.openFileOutput(filename, Context.MODE_PRIVATE).bufferedWriter().use {
            it.write(gson.toJson(posts))
        }
    }

    override fun getAll(): LiveData<List<Post>> =data

    override fun likedById(id: Int) {
        posts = posts.map {
            if (it.id != id) it else
                it.copy(likedByMe = !it.likedByMe, likes = if (!it.likedByMe) it.likes+1 else it.likes-1)
        }
        data.value = posts
        sync()
    }

    override fun sharedById(id: Int) {
        posts = posts.map {
            if (it.id != id) it else
                it.copy(sharedByMe = !it.sharedByMe, share = it.share+1)
        }
        data.value = posts
        sync()
    }

    override fun removedById(id: Int) {
        posts = posts.filter { it.id!=id }
        data.value = posts
        sync()
    }

    override fun save(post: Post) {
        if(post.id==0){
            posts = listOf(post.copy(
                id = nextId++,
                author = "Me",
                likedByMe = false,
                published = "now",
                sharedByMe = false
            )
            ) + posts
            data.value = posts
            return
        }
        posts = posts.map{
            if (it.id != post.id) it else it.copy (content = post.content, likes = post.likes, share = post.share)
        }
        data.value = posts
        sync()
    }
}