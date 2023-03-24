package com.mrapple100.bleremotecontrol

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.mrapple100.bleremotecontrol.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.ble.error.GattError
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private val BLP_SERVICE_UUID: UUID? = UUID.fromString("e7112e6c-c396-11ed-afa1-0242ac120002");//e7112e6c-c396-11ed-afa1-0242ac120002
    private val names:Array<String>? = arrayOf("RDB-1-test")
    private val mac:Array<String>? = arrayOf("0C:B8:15:F6:0D:52")


    val TAG ="CONTROLLER"


    private lateinit var bluetoothGattCallback: BluetoothGattCallback
    private lateinit var bluetoothScanner:BluetoothScanner
    private lateinit var activityViewModel: ActivityViewModel

    companion object{
        private var status:Int=-1;
        private var newState:Int=-1;
        private lateinit var gatt:BluetoothGatt;
        private var isOffMotors=false;

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProvider(this).get(
            ActivityViewModel::class.java
        )
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.vm =activityViewModel;
        binding.setLifecycleOwner(this);
        setContentView(binding.root)

        bluetoothScanner = BluetoothScanner(activityViewModel,this);

        val scanningButton = findViewById<Button>(R.id.Scan);
        scanningButton.setOnClickListener(View.OnClickListener {
            Scanning()
        })
        val connectButton = findViewById<Button>(R.id.Connect);
        connectButton.setOnClickListener(View.OnClickListener {
            Connect()
        })
        val OnOffMotorsButton = findViewById<Button>(R.id.OnOffMotors);
        OnOffMotorsButton.setOnClickListener(View.OnClickListener {
            OnOffMotors()
        })
        val position1Button = findViewById<Button>(R.id.Position1);
        position1Button.setOnClickListener(View.OnClickListener {
            Position1()
        })
        val position2Button = findViewById<Button>(R.id.Position2);
        position2Button.setOnClickListener(View.OnClickListener {
            Position2()
        })
        val position3Button = findViewById<Button>(R.id.Position3);
        position3Button.setOnClickListener(View.OnClickListener {
            Position3()
        })

        bluetoothGattCallback = object : BluetoothGattCallback(){
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                MainActivity.newState = newState
                MainActivity.status = status
                if(newState==BluetoothProfile.STATE_CONNECTED)
                    MainActivity.gatt = gatt!!


            }
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                val service = gatt!!.getService(BLP_SERVICE_UUID)
                service.characteristics.stream().forEach({it->Log.d(TAG, ""+it.uuid+" "+it.properties+" "+it.instanceId+" ")}).toString()
                Log.d(TAG, service.characteristics[0].uuid.toString())

                val characteristic0 = service.getCharacteristic(service.characteristics[0].uuid)
                Log.d(TAG,characteristic0.toString())
                var value :ByteArray? = null

               if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                   return
               }

                   runBlocking(Dispatchers.Main) {
                       activityViewModel.statemotors.value = "Calibration"
                   }
                Thread.sleep(10000)

                gatt.readCharacteristic(service.characteristics[1])






            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                if (status === BluetoothGatt.GATT_SUCCESS) {
                    // Получаем данные характеристики
                    var value = characteristic!!.value.clone()
                        if(value[0]==0.toByte() && value[1]==0.toByte()){//Если все двигатели выключены
                            isOffMotors=true;
                            //и включаем двигатели
                            value = byteArrayOf(0b11111111.toByte(), 0b00001111.toByte())
                            val service = gatt!!.getService(BLP_SERVICE_UUID)
                            val characteristic0 = service.characteristics[0];
                            characteristic0.value = value!!
                            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                return
                            }
                            gatt!!.writeCharacteristic(characteristic0)
                            runBlocking(Dispatchers.Main) {
                                activityViewModel.statemotors.value = "ON"
                            }
                        }
                    // Делаем что-то с полученными данными
                } else {
                    // Чтение характеристики не удалось
                }
            }
        }

    }


    fun longToBytes(x: Long): ByteArray? {
        val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putLong(x)
        return buffer.array()
    }





    fun Scanning(){
        println("Scanning")
        var filters: ArrayList<ScanFilter> = ArrayList();

        if (names != null) {
            for (name in names) {
                println(name)
                val filter = ScanFilter.Builder()
                    .setDeviceAddress(mac!![0])
                    .setDeviceName(name)
                    .build()
                filters.add(filter)
            }
        }

            if (ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPerms();
            }else
            if (ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                requestPerms();
            }else
//            if (ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
//                requestPerms();
//            }else
            if (ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPerms();
            }else
            if (ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPerms();
            }else
            if (ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPerms();
            }else {
                bluetoothScanner.startScan(filters)
                Log.d(TAG, "scan started");

            }

    }




    fun Connect(){

       if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val gatt: BluetoothGatt = activityViewModel.foundDevice.value!!.connectGatt(this, false, bluetoothGattCallback)

    }
    fun Disconnect(){

    }
    fun OnOffMotors(){
        if (ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPerms();
        }
        if(status == GattError.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Мы подключились, можно запускать обнаружение сервисов
                gatt?.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Мы успешно отключились (контролируемое отключение)
                gatt?.close();
            } else {
                // мы или подключаемся или отключаемся, просто игнорируем эти статусы
            }
        } else {
            // Произошла ошибка... разбираемся, что случилось!

            gatt?.close();
        }
    }
    fun Position1(){
        val pos1 = arrayListOf<kotlin.collections.ArrayList<Float>>( arrayListOf(0.8f, 12f, 0f, 10f, 0f ), arrayListOf( 0.9f, 10f, 0f, 10f, 0f ), arrayListOf( -0.8f, 10f, 0f, 4f, 0f ), arrayListOf( -0.8f, 12f, 0f, 10f, 0f ), arrayListOf( -0.9f, 10f, 0f, 10f, 0f ), arrayListOf(0.8f, 10f, 0f, 4f, 0f ), arrayListOf( -0.8f, 12f, 0f, 10f, 0f ), arrayListOf( 0.9f, 10f, 0f, 10f, 0f ), arrayListOf( -0.8f, 10f, 0f, 4f, 0f ), arrayListOf( 0.8f, 12f, 0f, 10f, 0f ), arrayListOf( -0.9f, 10f, 0f, 10f, 0f ), arrayListOf( 0.8f, 10f, 0f, 4f, 0f ) )
        val service = gatt!!.getService(BLP_SERVICE_UUID)
        val characteristic = service.getCharacteristic(service.characteristics[2].uuid)
        Log.d(TAG,characteristic.uuid.toString())
        var value :ByteArray? = null
        value =  ByteArray(pos1.size*pos1[0].size*Float.SIZE_BYTES)
        val buffer = ByteBuffer.wrap(value)
        for (i in pos1.indices){
            for(j in pos1[0].indices){
                buffer.putFloat(pos1[i][j])
            }
        }

        characteristic.value = value!!
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)
    }
    fun Position2(){

       val pos21 = arrayOf( doubleArrayOf( 0.8, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 1.4, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( -1.8, 10.0, 0.0, 4.0, 0.0),
        doubleArrayOf( -0.8, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( -1.4, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 1.8, 10.0, 0.0, 4.0, 0.0 ),
            doubleArrayOf( -0.8, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 1.4, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( -1.8, 10.0, 0.0, 4.0, 0.0 ),
            doubleArrayOf( 0.8, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( -1.4, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( -1.8, 10.0, 0.0, 4.0, 0.0 ) )
        val service = gatt!!.getService(BLP_SERVICE_UUID)
        val characteristic = service.getCharacteristic(service.characteristics[2].uuid)
        Log.d(TAG,characteristic.uuid.toString())
        var value :ByteArray? = null
        value =  ByteArray(pos21.size*pos21[0].size*Double.SIZE_BYTES)
        var buffer = ByteBuffer.wrap(value)
        for (i in pos21.indices){
            for(j in pos21[0].indices){
                buffer.putDouble(pos21[i][j])
            }
        }

        characteristic.value = value!!
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)

        Thread.sleep(3000)
        
       val pos22 =   arrayOf( doubleArrayOf(  0.8, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 0.9, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( -0.8, 10.0, 0.0, 4.0, 0.0 ), doubleArrayOf( -1.0, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 1.2, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 0.3, 10.0, 0.0, 4.0, 0.0 ), doubleArrayOf( -0.8, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 0.9, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( -0.8, 10.0, 0.0, 4.0, 0.0 ), doubleArrayOf( 0.8, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( -0.9, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 0.8, 10.0, 0.0, 4.0, 0.0 ) )

        value= null
        value =  ByteArray(pos22.size*pos22[0].size*Double.SIZE_BYTES)
        buffer = ByteBuffer.wrap(value)
        for (i in pos22.indices){
            for(j in pos22[0].indices){
                buffer.putDouble(pos22[i][j])
            }
        }

        characteristic.value = value!!
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)
    }
    fun Position3(){
        val pos31 = arrayOf( doubleArrayOf( 0.8, 12.0,0.0,10.0,0.0 ), doubleArrayOf( 0.9, 10.0,0.0,10.0,0.0 ), doubleArrayOf( -0.8, 10.0,0.0,4.0, 0.0 ), doubleArrayOf( -0.8, 12.0,0.0,10.0, 0.0 ), doubleArrayOf( -0.9, 10.0,0.0,10.0, 0.0 ), doubleArrayOf( 0.8, 10.0,0.0,4.0, 0.0 ), doubleArrayOf( -0.8, 12.0,0.0,10.0, 0.0 ), doubleArrayOf( 0.9, 10.0,0.0,10.0, 0.0 ), doubleArrayOf( -0.8, 10.0,0.0,4.0, 0.0 ), doubleArrayOf( 0.8, 12.0,0.0,10.0, 0.0 ), doubleArrayOf( -0.9, 10.0,0.0,10.0, 0.0 ), doubleArrayOf( 0.8, 10.0,0.0,4.0, 0.0 ) )

        val service = gatt!!.getService(BLP_SERVICE_UUID)
        val characteristic = service.getCharacteristic(service.characteristics[2].uuid)
        Log.d(TAG,characteristic.uuid.toString())
        var value :ByteArray? = null
        value =  ByteArray(pos31.size*pos31[0].size*Double.SIZE_BYTES)
        var buffer = ByteBuffer.wrap(value)
        for (i in pos31.indices){
            for(j in pos31[0].indices){
                buffer.putDouble(pos31[i][j])
            }
        }
        characteristic.value = value!!
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)

        Thread.sleep(3000)

        val pos32 = arrayOf( doubleArrayOf( 8.0, 12.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 0.9, 10.0, 0.0, 10.0,0.0), doubleArrayOf( -8.0, 10.0, 0.0, 4.0,0.0), doubleArrayOf( -8.0, 12.0, 0.0, 10.0,0.0), doubleArrayOf( -0.9, 10.0, 0.0, 10.0,0.0), doubleArrayOf( 8.0, 10.0, 0.0, 4.0,0.0), doubleArrayOf( -8.0, 12.0, 0.0, 10.0,0.0), doubleArrayOf( 0.9, 10.0, 0.0, 10.0,0.0), doubleArrayOf( -8.0, 10.0, 0.0, 4.0,0.0), doubleArrayOf( 1.6, 12.0, 0.0, 10.0,0.0), doubleArrayOf( -8.0, 10.0, 0.0, 10.0,0.0), doubleArrayOf( 0.0, 10.0, 0.0, 4.0,0.0) )

        value = null
        value =  ByteArray(pos32.size*pos32[0].size*Double.SIZE_BYTES)
        buffer = ByteBuffer.wrap(value)
        for (i in pos32.indices){
            for(j in pos32[0].indices){
                buffer.putDouble(pos32[i][j])
            }
        }
        characteristic.value = value!!
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)

        Thread.sleep(5000)


        val pos33 = arrayOf( doubleArrayOf( 8.0, 12.0, 0.0, 10.0,0.0), doubleArrayOf( 0.9, 10.0, 0.0, 10.0,0.0), doubleArrayOf( -8.0, 10.0, 0.0, 4.0,0.0), doubleArrayOf( -8.0, 12.0, 0.0, 10.0,0.0), doubleArrayOf( -0.9, 10.0, 0.0, 10.0,0.0), doubleArrayOf( 8.0, 10.0, 0.0, 4.0,0.0), doubleArrayOf( -8.0, 12.0, 0.0, 10.0,0.0), doubleArrayOf( 0.9, 10.0, 0.0, 10.0,0.0), doubleArrayOf( -8.0, 10.0, 0.0, 4.0,0.0), doubleArrayOf( 8.0, 12.0, 0.0, 10.0,0.0), doubleArrayOf( -0.9, 10.0, 0.0, 10.0, 0.0 ), doubleArrayOf( 8.0, 10.0, 0.0, 4.0, 0.0 ) )

        value = null
        value =  ByteArray(pos33.size*pos33[0].size*Double.SIZE_BYTES)
        buffer = ByteBuffer.wrap(value)
        for (i in pos33.indices){
            for(j in pos33[0].indices){
                buffer.putDouble(pos33[i][j])
            }
        }
        characteristic.value = value!!
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)
    }

    fun requestPerms() {
        val permble = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
        val permbl = arrayOf(
            Manifest.permission.BLUETOOTH,
        )
        val permloc = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val permloc2 = arrayOf(
             Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        val permloc3 = arrayOf(
             Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
       // requestMultiplePermissions.launch(permbl)
        requestMultiplePermissions.launch(permble)

        requestMultiplePermissions.launch(permloc)
        requestMultiplePermissions.launch(permloc2)
        requestMultiplePermissions.launch(permloc3)


        // ActivityCompat.requestPermissions(this as Activity, perm, 200)
        println("Permission")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode==200) {
            println("PERM" + Arrays.toString(permissions))
            println("PERM" + Arrays.toString(grantResults))
        }

    }
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }

}