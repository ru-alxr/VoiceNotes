package mx.alxr.voicenotes

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

}

data class Model(val popupMessage:String = "",
                 val isLoading:Boolean = false)