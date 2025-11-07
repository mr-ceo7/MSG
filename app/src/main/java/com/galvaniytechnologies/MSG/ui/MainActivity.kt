package com.galvaniytechnologies.MSG.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.galvaniytechnologies.MSG.R
import com.galvaniytechnologies.MSG.data.model.DeliveryStatus
import com.galvaniytechnologies.MSG.server.HttpServer
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val adapter = DeliveryLogAdapter()
    private lateinit var httpServer: HttpServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_logs)

        // Simulation toggle - allows safe end-to-end testing without sending real SMS
        val simulateSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_simulate)
        simulateSwitch.isChecked = com.galvaniytechnologies.MSG.util.DebugConfig.isSimulationEnabled(this)
        simulateSwitch.setOnCheckedChangeListener { _, isChecked ->
            com.galvaniytechnologies.MSG.util.DebugConfig.setSimulationEnabled(this, isChecked)
        }

        setupRecyclerView()
        setupTabs()
        setupObservers()
        startHttpServer()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.allLogs.observe(this@MainActivity) { logs ->
                        adapter.submitList(logs)
                    }
                    1 -> viewModel.getLogsByStatus(DeliveryStatus.PENDING.name)
                        .observe(this@MainActivity) { logs ->
                            adapter.submitList(logs)
                        }
                    2 -> viewModel.getLogsByStatus(DeliveryStatus.SENT.name)
                        .observe(this@MainActivity) { logs ->
                            adapter.submitList(logs)
                        }
                    3 -> viewModel.getLogsByStatus(DeliveryStatus.FAILED.name)
                        .observe(this@MainActivity) { logs ->
                            adapter.submitList(logs)
                        }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupObservers() {
        viewModel.allLogs.observe(this) { logs ->
            adapter.submitList(logs)
        }
    }

    private fun startHttpServer() {
        httpServer = HttpServer(applicationContext)
        httpServer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer.stop()
    }
}