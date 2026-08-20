package com.sam.talkdraft.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton

@Singleton(binds = [ISupabaseProvider::class])
class SupabaseProvider : ISupabaseProvider {

	override fun providesSupabase(): SupabaseClient {
		return createSupabaseClient(
			supabaseKey = BuildKonfig.SUPABASE_API_KEY,
			supabaseUrl = BuildKonfig.SUPABASE_API_URL
		) {
			install(Auth)
			defaultSerializer = KotlinXSerializer(json = Json)
		}
	}
}