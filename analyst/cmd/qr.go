// Copyright (c) BARGHEST
// SPDX-License-Identifier: AGPL-3.0-or-later

package cmd

import (
	"context"
	"flag"
	"fmt"
	"path/filepath"

	"github.com/BARGHEST-ngo/MESH/analyst/session"
	"github.com/peterbourgon/ff/v3/ffcli"
	"github.com/skip2/go-qrcode"
)

func QRCmd(s *session.Session) *ffcli.Command {
	fs := flag.NewFlagSet("qr", flag.ContinueOnError)
	out := fs.String("o", "", "save the QR code as a PNG to this file")

	return &ffcli.Command{
		Name:       "qr",
		ShortUsage: "qr [-o file.png]",
		ShortHelp:  "show the MESHnet address QR code, or save it as a PNG",
		FlagSet:    fs,
		Exec: func(ctx context.Context, args []string) error {
			return runQRCmd(s, out)
		},
	}
}

func runQRCmd(s *session.Session, out *string) error {
	if *out == "" {
		qr, err := qrcode.New(s.Address(), qrcode.Low)
		if err != nil {
			return err
		}
		fmt.Println(qr.ToSmallString(false))
		return nil
	}

	if err := qrcode.WriteFile(s.Address(), qrcode.Medium, 512, *out); err != nil {
		return fmt.Errorf("save QR code: %w", err)
	}
	path, _ := filepath.Abs(*out)
	fmt.Printf("QR code saved to %s\n", path)
	return nil
}
