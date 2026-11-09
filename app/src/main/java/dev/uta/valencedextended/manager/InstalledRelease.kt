package dev.uta.valencedextended.manager

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.edit
import app.valenced.manager.domain.manager.PatchBundle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * valenced-extended: どのリリースのモジュールでパッチした Discord が入っているかを覚える。
 *
 * 上流は Discord の版が上がったときだけ更新とみなすが、この fork のリリースは同じ Discord の版のまま
 * モジュールだけが新しくなるので、リリースのタグも比べる。
 *
 * インストールはユーザーが確認画面で取り消せるので、パッチ直後は「入れようとしている」リリースとして覚え、
 * パッケージの更新時刻がそれより後になったときに「入っている」リリースとみなす。
 */
object InstalledRelease : KoinComponent {

    private val context: Context by inject()

    private val prefs by lazy { context.getSharedPreferences("valenced_extended", Context.MODE_PRIVATE) }

    /** パッチを当て終えてインストールする直前に呼ぶ。@param release カスタムバンドルなら null */
    fun installing(release: String?) = prefs.edit {
        putString(PENDING, release.orEmpty())
        putLong(PENDING_SINCE, System.currentTimeMillis())
    }

    /** [installed] に入っているリリースのタグ。分からなければ null */
    fun of(installed: PackageInfo): String? {
        val since = prefs.getLong(PENDING_SINCE, -1)
        if (since >= 0 && installed.lastUpdateTime >= since) {
            prefs.edit {
                putString(CURRENT, prefs.getString(PENDING, "").orEmpty())
                remove(PENDING)
                remove(PENDING_SINCE)
            }
        }
        return prefs.getString(CURRENT, null)?.takeIf { it.isNotEmpty() }
    }

    /** [installed] が [bundle] のリリースより古いモジュールで作られているか。Discord の版は見ない */
    fun isOutdated(installed: PackageInfo?, bundle: PatchBundle?): Boolean {
        val release = bundle?.release ?: return false
        return installed != null && of(installed) != release
    }

    private const val PENDING = "pending_release"
    private const val PENDING_SINCE = "pending_release_since"
    private const val CURRENT = "installed_release"
}
