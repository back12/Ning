package de.csicar.ning

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.csicar.ning.ui.RecyclerViewCommon
import de.csicar.ning.util.AppPreferences
import de.csicar.ning.util.CopyUtil
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [NetworkFragment.OnListFragmentInteractionListener] interface.
 */
class NetworkFragment : Fragment() {
    private var listener: OnListFragmentInteractionListener? = null
    val viewModel : ScanViewModel by activityViewModels()

    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var emptyListInfo: View

    private lateinit var argumentInterfaceName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network_list, container, false)
        emptyListInfo = view.findViewById<View>(R.id.swipeDownViewImage)
        swipeRefreshLayout = view.findViewById(R.id.swipeDownView)
        argumentInterfaceName = arguments?.getString("interface_name")!!

        val copyUtil = CopyUtil(view)


        viewModel.devices.observe(viewLifecycleOwner) {
            emptyListInfo.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        viewModel.scanProgress.observe(viewLifecycleOwner) {
            when (it) {
                is ScanRepository.ScanProgress.ScanFinished -> {
                    progressBar.visibility = View.INVISIBLE
                    swipeRefreshLayout.isRefreshing = false
                }

                is ScanRepository.ScanProgress.ScanRunning -> {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = (it.progress * 1000.0).roundToInt()
                }

                is ScanRepository.ScanProgress.ScanNotStarted -> progressBar.visibility =
                    View.INVISIBLE
            }
        }

        val devicesList = view.findViewById<RecyclerViewCommon>(R.id.devicesList)
        devicesList.setHandler(
            requireContext(),
            this,
            object : RecyclerViewCommon.Handler<DeviceWithName>(
                R.layout.fragment_device,
                viewModel.devices
            ) {
                override fun bindItem(view: View): (DeviceWithName) -> Unit {
                    val ipTextView: TextView = view.findViewById(R.id.ipTextView)
                    val macTextView: TextView = view.findViewById(R.id.macTextView)
                    val vendorTextView: TextView = view.findViewById(R.id.vendorTextView)
                    val deviceNameTextView: TextView = view.findViewById(R.id.deviceNameTextView)
                    val deviceIcon: ImageView = view.findViewById(R.id.device_icon)

                    copyUtil.makeTextViewCopyable(macTextView)

                    return { item ->
                        ipTextView.text = item.ip.hostAddress
                        macTextView.text = item.hwAddress?.getAddress(
                            AppPreferences(
                                this@NetworkFragment
                            ).hideMacDetails)
                        vendorTextView.text = item.vendorName
                        deviceNameTextView.text = if (item.isScanningDevice) {
                            getString(R.string.this_device)
                        } else {
                            item.deviceName
                        }
                        deviceIcon.setImageResource(item.deviceType.icon)
                    }
                }

                override fun onClickListener(view: View, value: DeviceWithName) {
                    listener?.onListFragmentInteraction(value, view)
                }

                override fun onLongClickListener(view: View, value: DeviceWithName): Boolean {
                    return value.ip.hostAddress?.let(copyUtil::copyText) ?: false
                }

                override fun shareIdentity(a: DeviceWithName, b: DeviceWithName) =
                    a.deviceId == b.deviceId

                override fun areContentsTheSame(a: DeviceWithName, b: DeviceWithName) = a == b

            })

        swipeRefreshLayout.setOnRefreshListener {
            if(viewModel.scanProgress.value.isRunning) {
                Snackbar.make(requireView(), getString(R.string.network_scan_is_currently_running), Snackbar.LENGTH_LONG).show()
                return@setOnRefreshListener
            }
            runScan()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val scanOnStartup = AppPreferences(this).startScanOnStartup
        val scanNotStarted = viewModel.scanProgress.value == ScanRepository.ScanProgress.ScanNotStarted
        if(scanOnStartup && scanNotStarted) {
            runScan()
        }
    }

    private fun runScan() {
        viewModel.viewModelScope.launch {
            val network = viewModel.startScan(argumentInterfaceName)
            val view = this@NetworkFragment.view
            if (network == null && view != null) {
                Snackbar.make(view, getString(R.string.error_network_not_found), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: DeviceWithName, view: View)
    }
}
