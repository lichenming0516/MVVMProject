package cm.mvvm.core.base

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cm.mvvm.core.base.base.BaseVMFragment
import cm.mvvm.core.base.event.LoadingStatus
import cm.mvvm.core.utils.StatusBarUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.SizeUtils
import com.lcm.mvvmbase.R
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import java.lang.reflect.ParameterizedType

/**
 * ****************************************************************
 * Author: LiChenMing.Chaman
 * Date: 2019/11/1 14:56
 * Desc:
 * *****************************************************************
 */
abstract class BaseDialogFragment<DB : ViewDataBinding, VM : BaseViewModel> : DialogFragment(), BaseVMFragment {

    lateinit var viewDataBinding: DB
    lateinit var viewModel: VM
    private var instance: Any? = null

    private var viewModelFactory: ViewModelProvider.NewInstanceFactory? = null
    val lifecycleScopeProvider: AndroidLifecycleScopeProvider by lazy {
        AndroidLifecycleScopeProvider.from(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.BASE_ThemeDialog)
        if (needEventBus()) registerEventBus()
        viewModel = viewModel()
        viewModel.lifecycleScopeProvider = AndroidLifecycleScopeProvider.from(this)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (instance == null) {
            return null
        }
        viewDataBinding = DataBindingUtil.inflate(inflater, layoutId(), container, false)
        viewDataBinding.lifecycleOwner = this
        return viewDataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
        initLoadingView()
        setListener()
        baseObserve()
        observe()
        initData()
    }

    override fun onResume() {
        super.onResume()
        if (instance == null) {
            dismissAllowingStateLoss()
        }
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(activity, theme) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                return super.dispatchTouchEvent(ev)
            }
        }
    }


    override fun baseObserve() {
        viewModel.vmEvent.observe(this, Observer {
            handleVMEvent(it?.getContentIfNotHandled())
        })
        viewModel.loadStatus.observe(this, Observer {
            handleLoadingStatus(it?.getContentIfNotHandled())
        })
        viewModel.toastMsg.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { msg ->
                showToast(msg)
            }
        })
        viewModel.openPage.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { pair ->
                openPage(pair.first, pair.second)
            }
        })
        viewModel.openDialog.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { pair ->
                openDialog(pair.first, pair.second)
            }
        })
    }

    /**
     *设置ViewModelFactory
     */
    fun createViewModelFactory() {
        viewModelFactory = null
    }


    /**
     * 获取ViewModel
     */
    private fun viewModel(): VM {
        viewModel = if (viewModelFactory == null) {
            ViewModelProvider(activity!!).let {
                if(viewModelTag() == null) {
                    it.get(getVMClass())
                }else{
                    it.get("BaseFragment : $tag : ${viewModelTag()}",getVMClass())
                }
            }
        } else {
            ViewModelProvider(activity!!, viewModelFactory!!).let {
                if(viewModelTag() == null){
                    it.get(getVMClass())
                }else{
                    it.get("BaseFragment : $tag : ${viewModelTag()}",getVMClass())
                }
            }
        }
        return viewModel
    }

    /**
     * 获取ViewModel的class类型
     */
    @Suppress("UNCHECKED_CAST")
    private fun getVMClass(): Class<VM> {
        val type = this.javaClass.genericSuperclass as ParameterizedType
        return type.actualTypeArguments[1] as Class<VM>//<T>
    }


    fun fixedShow(activity: FragmentActivity) {
        fixedShow(activity, this.toString())
    }

    fun fixedShow(activity: FragmentActivity?, tag: String) {
        if (activity == null || activity.isFinishing) {
            Log.d("BaseDialogFragment", String.format("activity [%s] is null or is finishing!", activity))
            return
        }
        instance = Any()
        val ft = activity.supportFragmentManager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    /**
     * 此方法使用commit会出现异常 java.lang.IllegalStateException: Can not perform this
     * action after onSaveInstanceState
     */
    @Deprecated("此方法使用commit会出现异常", ReplaceWith("fixedShow(activity, tag)"))
    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
    }

    /**
     * 同 [.show]
     */
    @Deprecated("此方法使用commit会出现异常", ReplaceWith("fixedShow(activity, tag)"))
    override fun show(transaction: FragmentTransaction, tag: String?): Int {
        return super.show(transaction, tag)
    }


    override fun initLoadingView() {}
    override fun setListener() {}
    override fun observe() {}
    override fun openPage(page: String, param: Any?) {}
    override fun openDialog(dialog: String, param: Any?) {}
    override fun handleVMEvent(any: Any?) {}
    override fun handleLoadingStatus(loadingStatus: LoadingStatus?) {}
    override fun needEventBus(): Boolean = false
    override fun viewModelTag(): String? = null
    override fun registerEventBus() {}
    override fun unRegisterEventBus() {}

    override fun showToast(msg: String, duration: Int) {
        Toast.makeText(activity?.applicationContext, msg, duration).show()
    }

    override fun onDestroy() {
        if (needEventBus()) unRegisterEventBus()
        super.onDestroy()
        BaseApplication.refWatcher?.watch(this)
    }


    /************************************状态栏相关*****************************************/
    private fun setStatusBar() {
        if (useImmersiveStatusBar()) {
            if (fakeView() == null) {
                BarUtils.setStatusBarColor(activity!!, Color.TRANSPARENT)
            } else {
                if (BarUtils.getStatusBarHeight() > SizeUtils.dp2px(20f)) {
                    val layoutParams = fakeView()!!.layoutParams
                    layoutParams.height = BarUtils.getStatusBarHeight()
                    fakeView()!!.layoutParams = layoutParams
                }
                BarUtils.setStatusBarColor(fakeView()!!, statusBarColor())
            }
        }
    }

    private fun setStatusBarMode() {
        if (useImmersiveStatusBar()) {
            StatusBarUtils.setStatusBarLightMode(activity!!, statusBarIsDarkMode())
        }
    }

    override fun useImmersiveStatusBar(): Boolean = false
    override fun statusBarColor(): Int = Color.WHITE
    override fun fakeView(): View? = null
    override fun statusBarIsDarkMode(): Boolean = true

    /************************************状态栏相关*****************************************/


}