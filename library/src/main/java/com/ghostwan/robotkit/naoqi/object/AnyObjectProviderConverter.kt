package com.ghostwan.robotkit.naoqi.`object`

import com.aldebaran.qi.QiConversionException
import com.aldebaran.qi.sdk.`object`.AnyObjectProvider
import com.aldebaran.qi.serialization.QiSerializer
import java.lang.reflect.Type

class AnyObjectProviderConverter : QiSerializer.Converter {

    override fun canSerialize(o: Any): Boolean {
        return o is AnyObjectProvider
    }

    @Throws(QiConversionException::class)
    override fun serialize(qiSerializer: QiSerializer, o: Any): Any {
        return (o as AnyObjectProvider).anyObject
    }

    override fun canDeserialize(o: Any, type: Type): Boolean {
        // Default : a AnyObjectWrapper cannot be retrieved from the head
        return false
    }

    @Throws(QiConversionException::class)
    override fun deserialize(qiSerializer: QiSerializer, o: Any, type: Type): Any? {
        return null
    }
}