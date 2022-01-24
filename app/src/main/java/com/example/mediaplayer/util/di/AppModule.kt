package com.example.mediaplayer.util.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mediaplayer.R
import com.example.mediaplayer.view.adapter.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.upstream.DefaultDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Will migrate some to ServiceComponent

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .placeholder(R.drawable.splash_image_24_transparent)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))

    @Singleton
    @Provides
    fun provideAppContext(
        @ApplicationContext context: Context
    ) = context

    @Singleton
    @Provides
    fun provideAudioAttributes() = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    @Singleton
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ) = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(audioAttributes, true)
        setHandleAudioBecomingNoisy(true)
    }

    @Singleton
    @Provides
    fun provideDataSourceFactory(
        @ApplicationContext context: Context
    ) = DefaultDataSource.Factory(context)

    @Named("songAdapterNS")
    @Provides
    fun provideSongAdapterNS(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = SongAdapter(glide, context)

    @Named("folderAdapterNS")
    @Provides
    fun provideFolderAdapterNS(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = FolderAdapter(glide, context)

    @Named("homeAdapterNS")
    @Provides
    fun provideHomeAdapterNS(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = HomeAdapter(glide, context)

    @Named("albumAdapterNS")
    @Provides
    fun provideAlbumAdapterNS(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = AlbumAdapter(glide, context)

    @Named("artistAdapterNS")
    @Provides
    fun provideArtistAdapterNS(
        @ApplicationContext context: Context,
        glide: RequestManager
    ) = ArtistAdapter(glide, context)
}