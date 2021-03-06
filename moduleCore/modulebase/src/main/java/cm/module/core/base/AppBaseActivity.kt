package cm.module.core.base

import android.util.Log
import androidx.databinding.ViewDataBinding
import cm.mvvm.core.base.BaseActivity
import cm.mvvm.core.base.BaseViewModel
import cm.mvvm.core.base.event.LoadingStatus
import cm.mvvm.core.utils.RxBus

/**
 * ****************************************************************
 * Author: LiChenMing.Chaman
 * Date: 2019/11/1 15:26
 * Desc:
 * *****************************************************************
 */
abstract class AppBaseActivity<DB : ViewDataBinding, VM : BaseViewModel> : BaseActivity<DB, VM>() {

    override fun initLoadingView() {
        Log.e("TAG","初始化一个统一的LoadingView")
    }


    override fun handleLoadingStatus(loadingStatus: LoadingStatus?) {
        Log.e("TAG","统一处理 Loading状态")
    }

    override fun registerEventBus() {
        RxBus.get().register(this)
    }

    override fun unRegisterEventBus() {
        RxBus.get().unregister(this)
    }
}