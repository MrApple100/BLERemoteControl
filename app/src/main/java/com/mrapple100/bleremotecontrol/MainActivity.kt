package com.mrapple100.bleremotecontrol

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.*
import android.bluetooth.le.ScanFilter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.mrapple100.bleremotecontrol.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.util.*


class MainActivity : AppCompatActivity() {
    private val BLP_SERVICE_UUID: UUID? = UUID.fromString("e7112e6c-c396-11ed-afa1-0242ac120002");//e7112e6c-c396-11ed-afa1-0242ac120002
    private val names:Array<String>? = arrayOf("RDB-1")
    private val mac:Array<String>? = arrayOf("0C:B8:15:F6:0D:52")


    val TAG ="CONTROLLER"


    private lateinit var bluetoothGattCallback: BluetoothGattCallback
    private lateinit var bluetoothScanner:BluetoothScanner
    private lateinit var activityViewModel: ActivityViewModel

    private lateinit var service:BluetoothGattService;


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

                println("STATUS "+status)
                println(gatt!!.services.size)
                for(ser in gatt!!.services){
                    println(" "+ ser.uuid+" "+ ser.characteristics.toString()+" "+ser.type+" "+ser.includedServices)
                }
                println(gatt!!.services[2].getCharacteristic(UUID.fromString("ffffff00-0000-1000-8000-00805f9b34fb")))
                println("UUID "+UUID.fromString("ffffff00-0000-1000-8000-00805f9b34fb"))
                println(gatt!!.services.size)

                    service = gatt!!.getService(BLP_SERVICE_UUID)

//                if(service.getCharacteristic(UUID.fromString("ffffff00-0000-1000-8000-00805f9b34fb"))==null){
//                    service.characteristics.clear()
//                    service.addCharacteristic(BluetoothGattCharacteristic(UUID.fromString("ffffff00-0000-1000-8000-00805f9b34fb"),10,0))
//                    service.addCharacteristic(BluetoothGattCharacteristic(UUID.fromString("ffffff01-0000-1000-8000-00805f9b34fb"),2,0))
//                    service.addCharacteristic(BluetoothGattCharacteristic(UUID.fromString("ffffff02-0000-1000-8000-00805f9b34fb"),8,0))
//                }
                for(ser in gatt!!.services){
                    println("CHARASER "+ ser.uuid+" "+ ser.characteristics.toString()+" "+ser.type+" "+ser.includedServices)
                }
                for(chara in service.characteristics) {
                    println(TAG + " " + chara.uuid + " " + chara.properties + " " + chara.permissions + " ")
                }
                val characteristic0 = service.getCharacteristic(service.characteristics[0].uuid)
                Log.d(TAG,characteristic0.toString())

               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                   return
               }

                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        activityViewModel.statemotors.value = "Calibration"
                    }
                }
               // Thread.sleep(10000)

                gatt.readCharacteristic(service.characteristics[0])//заменил 1 на 0






            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                println(" "+status + " "+ characteristic!!.uuid)
                if (status === BluetoothGatt.GATT_SUCCESS) {
                    // Получаем данные характеристики
                    var value = characteristic!!.value.clone()
                    println(Arrays.toString(value))
                        if(value[0]==(0).toByte() && value[1]==(0).toByte()) {//Если все двигатели выключены
                            isOffMotors = true;
                            //и включаем двигатели
                            value = byteArrayOf(0b11111111.toByte(), 0b00001111.toByte())
                            val characteristic0 = service.characteristics[0];
                            characteristic0.value = value!!
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                return
                            }
                            gatt!!.writeCharacteristic(characteristic0)


                        }else{
                            CoroutineScope(Dispatchers.IO).launch {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, "Двигатели уже включены!", Toast.LENGTH_SHORT).show()
                                    activityViewModel.statemotors.value = "ON"
                                }
                            }
                        }
                    // Делаем что-то с полученными данными
                } else {
                    // Чтение характеристики не удалось
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                println(characteristic == service.characteristics[0])
                if (status === BluetoothGatt.GATT_SUCCESS) {
                    if (characteristic == service.characteristics[0]){

                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                activityViewModel.statemotors.value = "ON"
                            }
                        }
                    } else if (characteristic == service.characteristics[2]) {
                        println("VALUE "+Arrays.toString(characteristic!!.value))

                        val floats = FloatArray(characteristic.value.size/4);
                            var i = 0
                            while (i*4 < characteristic.value.size) {
                                val myFloat = ByteBuffer.wrap(characteristic.value).getFloat(i*4)
                                floats.set(i,myFloat)
                                i += 1
                            }
                        println("FLOAT "+Arrays.toString(floats))
                    }
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
                   // .setDeviceAddress(mac!![0])
                    .setDeviceName(name)
                    .build()
                filters.add(filter)
            }
        }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                requestPermsBLE();
            }else
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                requestPermsBLE();
            }else
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermsBLE();
            }else
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermsBLE();
            }else
            if ( ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermsLoc();
            }else
            if ( ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermsLoc();
            }else
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermsLoc();
            }else {
                bluetoothScanner.startScan(filters)
                Log.d(TAG, "scan started");

            }

    }




    fun Connect(){

       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            pairDevice(activityViewModel.foundDevice.value)
        }

        if(activityViewModel.foundDevice.value!=null) {
            val gatt: BluetoothGatt = activityViewModel.foundDevice.value!!
                .connectGatt(this, false, bluetoothGattCallback, TRANSPORT_AUTO
            )
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                gatt.readPhy()
            }
        }

    }
    fun Disconnect(){

    }
    fun OnOffMotors(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermsBLE();
        }
        if(status == BluetoothGatt.GATT_SUCCESS) {
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

    fun obrFloat( value:ByteArray):ByteArray{
        var i=0;

        while(i<value.size){
            var v3:Byte
            v3 = value[i+3]
            value[i+3] = value[i]
            value[i] = v3

            v3 = value[i+2]
            value[i+2] = value[i+1]
            value[i+1] = v3

            i+=4;
        }
        return value;
    }

    fun Position1(){
        val pos1 = arrayListOf<kotlin.collections.ArrayList<Float>>( arrayListOf(0.8f, 12f, 0f, 10f, 0f ), arrayListOf( 0.9f, 10f, 0f, 10f, 0f ), arrayListOf( -0.8f, 10f, 0f, 4f, 0f ), arrayListOf( -0.8f, 12f, 0f, 10f, 0f ), arrayListOf( -0.9f, 10f, 0f, 10f, 0f ), arrayListOf(0.8f, 10f, 0f, 4f, 0f ), arrayListOf( -0.8f, 12f, 0f, 10f, 0f ), arrayListOf( 0.9f, 10f, 0f, 10f, 0f ), arrayListOf( -0.8f, 10f, 0f, 4f, 0f ), arrayListOf( 0.8f, 12f, 0f, 10f, 0f ), arrayListOf( -0.9f, 10f, 0f, 10f, 0f ), arrayListOf( 0.8f, 10f, 0f, 4f, 0f ) )
        //val pos1 = arrayListOf(FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}),FloatArray(5,{i ->0f}))
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

        println("START "+Arrays.toString(value))

        value = obrFloat(value)
        characteristic.value = value!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)
       // gatt!!.writeCharacteristic(service.characteristics[2])
    }
    fun Position2(){

       val pos21 = arrayOf( floatArrayOf( 0.8f, 12.0f, 0.0f, 10.0f, 0.0f ), floatArrayOf( 1.4f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -1.8f, 10.0f, 0.0f, 4.0f, 0.0f),
        floatArrayOf( -0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -1.4f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 1.8f, 10.0f, 0.0f, 4.0f,0.0f),
            floatArrayOf( -0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 1.4f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -1.8f, 10.0f, 0.0f, 4.0f,0.0f),
            floatArrayOf( 0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -1.4f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -1.8f, 10.0f, 0.0f, 4.0f,0.0f) )
        val characteristic = service.getCharacteristic(service.characteristics[2].uuid)
        Log.d(TAG,characteristic.uuid.toString())
        var value :ByteArray? = null
        value =  ByteArray(pos21.size*pos21[0].size*Float.SIZE_BYTES)
        var buffer = ByteBuffer.wrap(value)
        for (i in pos21.indices){
            for(j in pos21[0].indices){
                buffer.putFloat(pos21[i][j])
            }
        }
        value = obrFloat(value)

        characteristic.value = value!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)

        Thread.sleep(3000)
        
       val pos22 =   arrayOf( floatArrayOf(  0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.8f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( -1.0f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 1.2f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.3f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( -0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.8f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( 0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.8f, 10.0f, 0.0f, 4.0f,0.0f) )

        value= null
        value =  ByteArray(pos22.size*pos22[0].size*Float.SIZE_BYTES)
        buffer = ByteBuffer.wrap(value)
        for (i in pos22.indices){
            for(j in pos22[0].indices){
                buffer.putFloat(pos22[i][j])
            }
        }
        value = obrFloat(value)

        characteristic.value = value!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)
    }
    fun Position3(){
        val pos31 = arrayOf( floatArrayOf( 0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.8f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( -0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.8f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( -0.8f, 12.0f,0.0f,10.0f,0.0f), floatArrayOf( 0.9f, 10.0f,0.0f,10.0f,0.0f), floatArrayOf( -0.8f, 10.0f,0.0f,4.0f,0.0f), floatArrayOf( 0.8f, 12.0f,0.0f,10.0f,0.0f), floatArrayOf( -0.9f, 10.0f,0.0f,10.0f,0.0f), floatArrayOf( 0.8f, 10.0f,0.0f,4.0f,0.0f) )

        val characteristic = service.getCharacteristic(service.characteristics[2].uuid)
        Log.d(TAG,characteristic.uuid.toString())
        var value :ByteArray? = null
        value =  ByteArray(pos31.size*pos31[0].size*Float.SIZE_BYTES)
        var buffer = ByteBuffer.wrap(value)
        for (i in pos31.indices){
            for(j in pos31[0].indices){
                buffer.putFloat(pos31[i][j])
            }
        }
        value = obrFloat(value)

        characteristic.value = value!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)

        Thread.sleep(3000)
//пиздец тут большие проблемы
// перепроверить!!!!!!!!!!!!!!!!!
            //       val pos32 = arrayOf( floatArrayOf( 0.0f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.8f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( -0.8f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.8f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( -8.0f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.8f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( 1.6f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.8f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.0f, 10.0f, 0.0f, 4.0f,0.0f) )

        value = null
        value =  ByteArray(pos32.size*pos32[0].size*Float.SIZE_BYTES)
        buffer = ByteBuffer.wrap(value)
        for (i in pos32.indices){
            for(j in pos32[0].indices){
                buffer.putFloat(pos32[i][j])
            }
        }
        value = obrFloat(value)

        characteristic.value = value!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)

        Thread.sleep(5000)


        val pos33 = arrayOf( floatArrayOf( 8.0f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -8.0f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( -8.0f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 8.0f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( -8.0f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -8.0f, 10.0f, 0.0f, 4.0f,0.0f), floatArrayOf( 8.0f, 12.0f, 0.0f, 10.0f,0.0f), floatArrayOf( -0.9f, 10.0f, 0.0f, 10.0f,0.0f), floatArrayOf( 8.0f, 10.0f, 0.0f, 4.0f,0.0f) )

        value = null
        value =  ByteArray(pos33.size*pos33[0].size*Float.SIZE_BYTES)
        buffer = ByteBuffer.wrap(value)
        for (i in pos33.indices){
            for(j in pos33[0].indices){
                buffer.putFloat(pos33[i][j])
            }
        }
        value = obrFloat(value)

        characteristic.value = value!!
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt!!.writeCharacteristic(characteristic)
    }

    fun requestPermsBLE() {
        val permble = arrayListOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        //permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //  >= android 12

            permble.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { //  >= android 12
                requestPermissions(permble.toTypedArray(),200);
          }else {

              // requestMultiplePermissions.launch(permbl)
              requestMultiplePermissions.launch(permble.toTypedArray())


          }

        // ActivityCompat.requestPermissions(this as Activity, perm, 200)
        println("Permission")
    }
    fun requestPermsLoc(){
        val permloc = arrayListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //  >= android 12

           permloc.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { //  >= android 12
            requestPermissions(permloc.toTypedArray(),200);
        }else {
            requestMultiplePermissions.launch(permloc.toTypedArray())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            this.startActivityForResult(enableLocationIntent, 200)
        }

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

    fun pairDevice(device: BluetoothDevice?) {
        val intent = Intent(ACTION_PAIRING_REQUEST)
        intent.putExtra(EXTRA_DEVICE, device)
        println("DEVICE "+device!!.name)
        intent.putExtra(EXTRA_PAIRING_VARIANT, PAIRING_VARIANT_PIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        this.startActivity(intent)
    }
}