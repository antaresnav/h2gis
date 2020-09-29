/**
 * H2GIS is a library that brings spatial support to the H2 Database Engine
 * <http://www.h2database.com>. H2GIS is developed by CNRS
 * <http://www.cnrs.fr/>.
 *
 * This code is part of the H2GIS project. H2GIS is free software; 
 * you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * H2GIS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details <http://www.gnu.org/licenses/>.
 *
 *
 * For more information, please consult: <http://www.h2gis.org/>
 * or contact directly: info_at_h2gis.org
 */

package org.h2gis.functions.io.dbf;

import org.h2.command.ddl.CreateTableData;
import org.h2.table.Column;
import org.h2.value.TypeInfo;
import org.h2.value.Value;
import org.h2.value.ValueNull;
import org.h2gis.functions.io.dbf.internal.DBFDriver;
import org.h2gis.functions.io.dbf.internal.DbaseFileHeader;
import org.h2gis.functions.io.file_table.FileEngine;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * SHP Table factory.
 * @author Nicolas Fortin
 */
public class DBFEngine extends FileEngine<DBFDriver> {

    @Override
    protected DBFDriver createDriver(File filePath, List<String> args) throws IOException {
        DBFDriver driver = new DBFDriver();
        driver.initDriverFromFile(filePath,  args.size() > 1 ? args.get(1) : null);
        return driver;
    }

    @Override
    protected void feedCreateTableData(DBFDriver driver, CreateTableData data) throws IOException {
        DbaseFileHeader header = driver.getDbaseFileHeader();
        feedTableDataFromHeader(header, data);
    }

    /**
     * Parse the DBF file then init the provided data structure
     * @param header dbf header
     * @param data Data to initialise
     * @throws java.io.IOException
     */
    public static void feedTableDataFromHeader(DbaseFileHeader header, CreateTableData data) throws IOException {
        for (int i = 0; i < header.getNumFields(); i++) {
            String fieldsName = header.getFieldName(i);
            final int type = dbfTypeToH2Type(header,i);
            TypeInfo typeInfo = new TypeInfo(type, header.getFieldLength(i), 0, TypeInfo.TYPE_VARCHAR);
            Column column = new Column(fieldsName.toUpperCase(), typeInfo);
            data.columns.add(column);
        }
    }

    /**
     * @see "http://www.clicketyclick.dk/databases/xbase/format/data_types.html"
     * @param header DBF File Header
     * @param i DBF Type identifier
     * @return H2 {@see Value}
     * @throws java.io.IOException
     */
    private static int dbfTypeToH2Type(DbaseFileHeader header, int i) throws IOException {
        switch (header.getFieldType(i)) {
            // (L)logical (T,t,F,f,Y,y,N,n)
            case 'l':
            case 'L':
                return Value.BOOLEAN;
            // (C)character (String)
            case 'c':
            case 'C':
                return Value.CHAR;
            // (D)date (Date)
            case 'd':
            case 'D':
                return Value.DATE;
            // (F)floating (Double)
            case 'n':
            case 'N':
                if ((header.getFieldDecimalCount(i) == 0)) {
                    if ((header.getFieldLength(i) >= 0)
                            && (header.getFieldLength(i) < 10)) {
                        return Value.INTEGER;
                    } else {
                        return Value.BIGINT;
                    }
                }
            case 'f':
            case 'F': // floating point number
            case 'o':
            case 'O': // floating point number
                return Value.DOUBLE;
            default:
                throw new IOException("Unknown DBF field type "+header.getFieldType(i));
        }
    }
}
