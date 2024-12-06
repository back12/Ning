package de.csicar.ning

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import de.csicar.ning.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), NetworkFragment.OnListFragmentInteractionListener {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val viewModel: ScanViewModel by viewModels()
    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.drawerNavigation.setupWithNavController(navController)
        setSupportActionBar(binding.toolbar)
        appBarConfiguration = AppBarConfiguration.Builder(setOf(R.id.deviceFragment, R.id.appPreferenceFragment))
            .setOpenableLayout(binding.mainDrawerLayout)
            .build()
        setupActionBarWithNavController(navController, appBarConfiguration)

        val interfaceMenu = binding.drawerNavigation.menu.addSubMenu(getString(R.string.interfaces_submenu))

        viewModel.fetchAvailableInterfaces().forEach { nic ->
            interfaceMenu.add("${nic.interfaceName} - ${nic.address.hostAddress}/${nic.prefix}").also {
                it.setOnMenuItemClickListener {
                    val bundle = bundleOf("interface_name" to nic.interfaceName)
                    navController.navigate(R.id.deviceFragment, bundle)
                    binding.mainDrawerLayout.closeDrawers()
                    true
                }
                it.setIcon(R.drawable.ic_settings_ethernet_white_24dp)
                it.isCheckable = true
                it.isEnabled = true
            }
        }
        val preferences = binding.drawerNavigation.menu.add(getString(R.string.preferences_submenu))
        preferences.setIcon(R.drawable.ic_settings_white_24dp)
        preferences.setOnMenuItemClickListener {
            navController.navigate(R.id.appPreferenceFragment)
            binding.mainDrawerLayout.closeDrawers()
            true
        }
    }


    override fun onListFragmentInteraction(item: DeviceWithName, view: View) {
        val bundle = bundleOf("deviceId" to item.deviceId, "deviceIp" to item.ip.hostAddress)
        navController.navigate(R.id.deviceInfoFragment, bundle)
    }
}
